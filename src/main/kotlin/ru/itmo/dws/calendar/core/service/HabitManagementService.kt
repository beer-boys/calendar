package ru.itmo.dws.calendar.core.service

import java.time.LocalDate
import java.time.ZoneId
import org.slf4j.LoggerFactory
import ru.itmo.dws.calendar.core.domain.model.CreateHabitRequest
import ru.itmo.dws.calendar.core.domain.model.EventType
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.HabitConflict
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.model.HabitSchedulePlan
import ru.itmo.dws.calendar.core.domain.model.UpdateHabitRequest
import ru.itmo.dws.calendar.core.domain.model.toHabitConflict
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.input.HabitCreationResult
import ru.itmo.dws.calendar.core.port.input.HabitCreationStatus
import ru.itmo.dws.calendar.core.port.input.HabitManagementUseCase
import ru.itmo.dws.calendar.core.port.input.HabitSyncResult
import ru.itmo.dws.calendar.core.port.output.HabitOccurrenceRepository
import ru.itmo.dws.calendar.core.port.output.HabitRepository
import ru.itmo.dws.calendar.core.service.provider.SchedulableEventProvider

@Suppress("LongParameterList", "TooManyFunctions")
class HabitManagementService(
    private val habitRepository: HabitRepository,
    private val occurrenceRepository: HabitOccurrenceRepository,
    private val eventProviders: List<SchedulableEventProvider>,
    private val eventSlotFinder: EventSlotFinder,
    private val conflictDetectionService: ConflictDetectionService,
    private val habitSchedulingService: HabitSchedulingService,
    private val habitSyncService: HabitSyncService,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : HabitManagementUseCase {

    private val log = LoggerFactory.getLogger(HabitManagementService::class.java)

    companion object {
        const val DEFAULT_PLANNING_WEEKS = 4
    }

    override fun createHabit(request: CreateHabitRequest): HabitCreationResult {
        val habit = request.toHabit()
        val today = LocalDate.now(zoneId)

        habitRepository.saveHabit(habit)

        val plan = habitSchedulingService.planSchedule(habit, DEFAULT_PLANNING_WEEKS)
        val syncResult = habitSyncService.syncOccurrencesToExternalCalendar(habit, plan.occurrences)

        log.info(
            "Created habit {} with {} occurrences ({} synced to external calendar)",
            habit.id,
            plan.totalCount,
            syncResult.syncedCount
        )

        val todayOccurrence = plan.occurrences.find { it.date == today }
        val scheduledSlot = todayOccurrence?.timeSlot

        val conflicts = if (scheduledSlot != null) {
            detectConflictsForHabit(habit, today)
        } else {
            emptyList()
        }

        val status = when {
            scheduledSlot != null && conflicts.isEmpty() -> HabitCreationStatus.CREATED_WITH_SLOT
            scheduledSlot != null -> HabitCreationStatus.CREATED_WITH_CONFLICTS
            else -> HabitCreationStatus.CREATED_WITHOUT_SLOT
        }

        return HabitCreationResult(
            habit = habit,
            scheduledSlot = scheduledSlot,
            conflicts = conflicts,
            status = status
        )
    }

    override fun getHabit(habitId: HabitId): Habit? {
        return habitRepository.findHabit(habitId)
    }

    override fun getHabits(userId: UserId): List<Habit> {
        return habitRepository.findHabits(userId)
    }

    override fun getHabitsForDate(userId: UserId, date: LocalDate): List<Habit> {
        return habitRepository.findHabitsForDate(userId, date)
    }

    override fun updateHabit(habitId: HabitId, request: UpdateHabitRequest): Habit {
        val existingHabit = habitRepository.findHabit(habitId)
            ?: throw IllegalArgumentException("Habit not found: $habitId")

        val updatedHabit = request.applyTo(existingHabit)

        habitRepository.updateHabit(habitId, updatedHabit)

        syncExistingOccurrencesToExternalCalendar(updatedHabit)

        return updatedHabit
    }

    override fun deleteHabit(habitId: HabitId) {
        val habit = habitRepository.findHabit(habitId)
        if (habit != null) {
            habitSyncService.deleteAllOccurrencesFromExternalCalendar(habit)
            log.info("Deleted all occurrences for habit {}", habitId)
        }
        habitRepository.deleteHabit(habitId)
    }

    override fun scheduleHabitForDate(habitId: HabitId, date: LocalDate, timeSlot: TimeSlot): Habit {
        val habit = habitRepository.findHabit(habitId)
            ?: throw IllegalArgumentException("Habit not found: $habitId")

        require(habit.isWithinFlexibilityWindow(timeSlot)) {
            "Time slot must be within flexibility window"
        }

        val rescheduledHabit = habit.reschedule(timeSlot)
        habitRepository.updateHabit(habitId, rescheduledHabit)

        return rescheduledHabit
    }

    override fun clearHabitScheduleForDate(habitId: HabitId, date: LocalDate): Habit {
        val habit = habitRepository.findHabit(habitId)
            ?: throw IllegalArgumentException("Habit not found: $habitId")

        val clearedHabit = habit.clearTimeSlot()
        habitRepository.updateHabit(habitId, clearedHabit)

        return clearedHabit
    }

    override fun planHabitSchedule(habitId: HabitId, weeks: Int): HabitSchedulePlan {
        val habit = habitRepository.findHabit(habitId)
            ?: throw IllegalArgumentException("Habit not found: $habitId")

        return habitSchedulingService.planSchedule(habit, weeks)
    }

    override fun syncHabitToExternalCalendar(habitId: HabitId, weeks: Int): HabitSyncResult {
        val habit = habitRepository.findHabit(habitId)
            ?: throw IllegalArgumentException("Habit not found: $habitId")

        val plan = habitSchedulingService.planSchedule(habit, weeks)

        val syncResult = habitSyncService.syncOccurrencesToExternalCalendar(habit, plan.occurrences)

        log.info(
            "Synced habit {} to external calendar: {} synced, {} failed, {} skipped",
            habitId,
            syncResult.syncedCount,
            syncResult.failedCount,
            syncResult.skippedCount
        )

        return HabitSyncResult(
            habitId = habitId,
            syncedCount = syncResult.syncedCount,
            failedCount = syncResult.failedCount,
            skippedCount = syncResult.skippedCount,
            occurrences = syncResult.occurrences
        )
    }

    override fun getHabitOccurrences(habitId: HabitId): List<HabitOccurrence> {
        return occurrenceRepository.findByHabitId(habitId)
    }

    override fun getHabitOccurrences(
        habitId: HabitId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitOccurrence> {
        return occurrenceRepository.findByHabitIdAndDateRange(habitId, startDate, endDate)
    }

    private fun detectConflictsForHabit(habit: Habit, date: LocalDate): List<HabitConflict> {
        return conflictDetectionService.detectAllConflictsForUser(habit.userId, date)
            .filter { it.sourceEvent.eventType == EventType.HABIT && it.sourceEvent.eventId == habit.id.toString() }
            .mapNotNull { it.toHabitConflict() }
    }

    private fun syncExistingOccurrencesToExternalCalendar(habit: Habit) {
        val existingOccurrences = occurrenceRepository.findByHabitId(habit.id)
            .filter { it.isSynced }

        if (existingOccurrences.isEmpty()) {
            return
        }

        val syncResult = habitSyncService.syncOccurrencesToExternalCalendar(habit, existingOccurrences)

        log.info(
            "Updated {} occurrences for habit {} in external calendar",
            syncResult.syncedCount,
            habit.id
        )
    }
}
