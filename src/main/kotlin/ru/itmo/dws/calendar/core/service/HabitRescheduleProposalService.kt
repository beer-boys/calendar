package ru.itmo.dws.calendar.core.service

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.HabitConflict
import ru.itmo.dws.calendar.core.domain.model.HabitRescheduleProposal
import ru.itmo.dws.calendar.core.domain.model.HabitRescheduleResult
import ru.itmo.dws.calendar.core.domain.model.RescheduleStatus
import ru.itmo.dws.calendar.core.domain.model.UserRescheduleDecision
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.input.HabitRescheduleProposalUseCase
import ru.itmo.dws.calendar.core.port.output.CalendarProvider
import ru.itmo.dws.calendar.core.port.output.FocusTimeRepository
import ru.itmo.dws.calendar.core.port.output.HabitRepository
import ru.itmo.dws.calendar.core.port.output.MeetingRepository

class HabitRescheduleProposalService(
    private val habitRepository: HabitRepository,
    private val meetingRepository: MeetingRepository,
    private val focusTimeRepository: FocusTimeRepository,
    private val calendarProvider: CalendarProvider,
    private val habitSlotFinder: HabitSlotFinder,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val config: RescheduleProposalConfig = RescheduleProposalConfig.default()
) : HabitRescheduleProposalUseCase {

    private val activeProposals = ConcurrentHashMap<String, HabitRescheduleProposal>()

    override fun generateProposal(conflict: HabitConflict): HabitRescheduleProposal {
        val habit = habitRepository.findHabit(conflict.habitId)
            ?: throw IllegalArgumentException("Habit not found: ${conflict.habitId}")

        val date = conflict.affectedDate
        val occupiedSlots = collectOccupiedSlotsExcludingHabit(habit, date)

        val allOccupiedSlots = occupiedSlots + conflict.conflictingEvent.timeSlot

        val proposedSlots = habitSlotFinder.generateProposedSlots(
            duration = habit.duration,
            flexibilityWindow = habit.flexibilityWindow,
            date = date,
            occupiedSlots = allOccupiedSlots,
            bufferTime = habit.bufferTime,
            preferredStartTime = habit.preferredStartTime(),
            maxSlots = config.maxProposedSlots,
            zoneId = zoneId
        )

        val alternativeDates = if (habit.canMoveToDifferentDay() && proposedSlots.isEmpty()) {
            findAlternativeDates(habit, date, config.maxAlternativeDays)
        } else {
            emptyList()
        }

        val proposal = HabitRescheduleProposal(
            habitId = habit.id,
            userId = habit.userId,
            conflict = conflict,
            proposedSlots = proposedSlots,
            alternativeDates = alternativeDates,
            canSkipForDate = true,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(config.proposalExpirationSeconds)
        )

        activeProposals[proposal.id] = proposal
        return proposal
    }

    override fun applyDecision(proposalId: String, decision: UserRescheduleDecision): HabitRescheduleResult {
        val proposal = activeProposals[proposalId]
            ?: return createFailedResult(
                proposalId = proposalId,
                status = RescheduleStatus.FAILED_PROPOSAL_EXPIRED,
                message = "Proposal '$proposalId' not found or expired"
            )

        if (proposal.isExpired()) {
            activeProposals.remove(proposalId)
            return createFailedResult(
                habitId = proposal.habitId,
                affectedDate = proposal.conflict.affectedDate,
                status = RescheduleStatus.FAILED_PROPOSAL_EXPIRED,
                message = "Proposal has expired"
            )
        }

        val habit = habitRepository.findHabit(proposal.habitId)
            ?: return createFailedResult(
                habitId = proposal.habitId,
                affectedDate = proposal.conflict.affectedDate,
                status = RescheduleStatus.FAILED_INVALID_SLOT,
                message = "Habit not found"
            )

        val result = when (decision) {
            is UserRescheduleDecision.AcceptSlot -> handleAcceptSlot(habit, proposal, decision.slotIndex)
            is UserRescheduleDecision.CustomSlot -> handleCustomSlot(habit, proposal, decision.timeSlot)
            is UserRescheduleDecision.SkipForDate -> handleSkipForDate(habit, proposal)
            is UserRescheduleDecision.MoveToDifferentDay -> handleMoveToDifferentDay(
                habit,
                proposal,
                decision.newDate,
                decision.timeSlot
            )
            is UserRescheduleDecision.KeepConflict -> handleKeepConflict(habit, proposal)
        }

        if (result.isSuccess) {
            activeProposals.remove(proposalId)
        }

        return result
    }

    override fun getActiveProposals(userId: UserId): List<HabitRescheduleProposal> {
        val now = Instant.now()
        return activeProposals.values
            .filter { it.userId == userId && !it.isExpired(now) }
            .toList()
    }

    override fun getProposal(proposalId: String): HabitRescheduleProposal? {
        val proposal = activeProposals[proposalId]
        return if (proposal != null && !proposal.isExpired()) proposal else null
    }

    override fun cancelProposal(proposalId: String) {
        activeProposals.remove(proposalId)
    }

    private fun handleAcceptSlot(
        habit: Habit,
        proposal: HabitRescheduleProposal,
        slotIndex: Int
    ): HabitRescheduleResult {
        val proposedSlot = proposal.getSlotByIndex(slotIndex)
            ?: return createFailedResult(
                habitId = habit.id,
                affectedDate = proposal.conflict.affectedDate,
                status = RescheduleStatus.FAILED_INVALID_SLOT,
                message = "Invalid slot index: $slotIndex"
            )

        return rescheduleHabit(habit, proposal, proposedSlot.timeSlot)
    }

    private fun handleCustomSlot(
        habit: Habit,
        proposal: HabitRescheduleProposal,
        timeSlot: TimeSlot
    ): HabitRescheduleResult {
        if (!habit.isWithinFlexibilityWindow(timeSlot)) {
            return createFailedResult(
                habitId = habit.id,
                affectedDate = proposal.conflict.affectedDate,
                status = RescheduleStatus.FAILED_INVALID_SLOT,
                message = "Time slot is outside flexibility window"
            )
        }

        val occupiedSlots = collectOccupiedSlotsExcludingHabit(habit, proposal.conflict.affectedDate)
        val effectiveSlot = if (habit.bufferTime.hasBuffer()) {
            timeSlot.withBuffer(habit.bufferTime)
        } else {
            timeSlot
        }

        if (occupiedSlots.any { it.overlapsWith(effectiveSlot) }) {
            return createFailedResult(
                habitId = habit.id,
                affectedDate = proposal.conflict.affectedDate,
                status = RescheduleStatus.FAILED_SLOT_OCCUPIED,
                message = "Selected time slot is occupied"
            )
        }

        return rescheduleHabit(habit, proposal, timeSlot)
    }

    private fun handleSkipForDate(
        habit: Habit,
        proposal: HabitRescheduleProposal
    ): HabitRescheduleResult {
        val clearedHabit = habit.clearTimeSlot()
        habitRepository.updateHabit(habit.id, clearedHabit)

        return HabitRescheduleResult(
            habitId = habit.id,
            status = RescheduleStatus.SKIPPED_FOR_DATE,
            previousTimeSlot = habit.currentTimeSlot,
            newTimeSlot = null,
            affectedDate = proposal.conflict.affectedDate,
            message = "Habit '${habit.title}' skipped for ${proposal.conflict.affectedDate}"
        )
    }

    private fun handleMoveToDifferentDay(
        habit: Habit,
        proposal: HabitRescheduleProposal,
        newDate: LocalDate,
        timeSlot: TimeSlot
    ): HabitRescheduleResult {
        if (!habit.canMoveToDifferentDay()) {
            return createFailedResult(
                habitId = habit.id,
                affectedDate = proposal.conflict.affectedDate,
                status = RescheduleStatus.FAILED_INVALID_SLOT,
                message = "Habit cannot be moved to a different day"
            )
        }

        if (!proposal.alternativeDates.contains(newDate)) {
            return createFailedResult(
                habitId = habit.id,
                affectedDate = proposal.conflict.affectedDate,
                status = RescheduleStatus.FAILED_INVALID_SLOT,
                message = "Date $newDate is not among alternative dates"
            )
        }

        if (!habit.isWithinFlexibilityWindow(timeSlot)) {
            return createFailedResult(
                habitId = habit.id,
                affectedDate = proposal.conflict.affectedDate,
                status = RescheduleStatus.FAILED_INVALID_SLOT,
                message = "Time slot is outside flexibility window"
            )
        }

        val rescheduledHabit = habit.reschedule(timeSlot)
        habitRepository.updateHabit(habit.id, rescheduledHabit)

        return HabitRescheduleResult(
            habitId = habit.id,
            status = RescheduleStatus.MOVED_TO_DIFFERENT_DAY,
            previousTimeSlot = habit.currentTimeSlot,
            newTimeSlot = timeSlot,
            affectedDate = newDate,
            message = "Habit '${habit.title}' moved to $newDate at ${timeSlot.start.toLocalTime()}"
        )
    }

    private fun handleKeepConflict(
        habit: Habit,
        proposal: HabitRescheduleProposal
    ): HabitRescheduleResult {
        return HabitRescheduleResult(
            habitId = habit.id,
            status = RescheduleStatus.KEPT_WITH_CONFLICT,
            previousTimeSlot = habit.currentTimeSlot,
            newTimeSlot = habit.currentTimeSlot,
            affectedDate = proposal.conflict.affectedDate,
            message = "Habit '${habit.title}' kept with conflict"
        )
    }

    private fun rescheduleHabit(
        habit: Habit,
        proposal: HabitRescheduleProposal,
        newTimeSlot: TimeSlot
    ): HabitRescheduleResult {
        val rescheduledHabit = habit.reschedule(newTimeSlot)
        habitRepository.updateHabit(habit.id, rescheduledHabit)

        return HabitRescheduleResult(
            habitId = habit.id,
            status = RescheduleStatus.RESCHEDULED,
            previousTimeSlot = habit.currentTimeSlot,
            newTimeSlot = newTimeSlot,
            affectedDate = proposal.conflict.affectedDate,
            message = "Habit '${habit.title}' rescheduled to ${newTimeSlot.start.toLocalTime()}"
        )
    }

    private fun collectOccupiedSlotsExcludingHabit(habit: Habit, date: LocalDate): List<TimeSlot> {
        val dayTimeSlot = createDayTimeSlot(date)

        val meetings = meetingRepository.findMeetings(habit.userId, dayTimeSlot)
        val habits = habitRepository.findHabitsForDate(habit.userId, date)
        val focusTimes = focusTimeRepository.findFocusTimes(habit.userId, dayTimeSlot)

        val calendarEvents = try {
            calendarProvider.getEvents(habit.userId, dayTimeSlot)
        } catch (e: Exception) {
            emptyList()
        }

        return habitSlotFinder.collectOccupiedSlots(
            meetings = meetings,
            habits = habits,
            focusTimes = focusTimes,
            calendarEvents = calendarEvents,
            excludeHabitId = habit.id
        )
    }

    private fun findAlternativeDates(habit: Habit, originalDate: LocalDate, maxDays: Int): List<LocalDate> {
        val alternativeDates = mutableListOf<LocalDate>()

        for (offset in 1..maxDays) {
            val nextDate = originalDate.plusDays(offset.toLong())
            if (habit.shouldOccurOn(nextDate)) {
                val occupiedSlots = collectOccupiedSlotsExcludingHabit(habit, nextDate)
                val availableSlot = habitSlotFinder.findOptimalSlot(
                    duration = habit.duration,
                    flexibilityWindow = habit.flexibilityWindow,
                    date = nextDate,
                    occupiedSlots = occupiedSlots,
                    bufferTime = habit.bufferTime,
                    zoneId = zoneId
                )
                if (availableSlot != null) {
                    alternativeDates.add(nextDate)
                }
            }
        }

        return alternativeDates
    }

    private fun createDayTimeSlot(date: LocalDate): TimeSlot {
        val startOfDay = date.atStartOfDay(zoneId)
        val endOfDay = date.plusDays(1).atStartOfDay(zoneId)
        return TimeSlot(startOfDay, endOfDay)
    }

    private fun createFailedResult(
        habitId: HabitId,
        affectedDate: LocalDate,
        status: RescheduleStatus,
        message: String
    ): HabitRescheduleResult {
        return HabitRescheduleResult(
            habitId = habitId,
            status = status,
            previousTimeSlot = null,
            newTimeSlot = null,
            affectedDate = affectedDate,
            message = message
        )
    }

    private fun createFailedResult(
        proposalId: String,
        status: RescheduleStatus,
        message: String
    ): HabitRescheduleResult {
        return HabitRescheduleResult(
            habitId = HabitId.generate(),
            status = status,
            previousTimeSlot = null,
            newTimeSlot = null,
            affectedDate = LocalDate.now(zoneId),
            message = message
        )
    }
}
