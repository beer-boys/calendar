package ru.itmo.dws.calendar.core.service

import java.time.LocalDate
import java.time.ZoneId
import ru.itmo.dws.calendar.core.domain.model.CreateHabitRequest
import ru.itmo.dws.calendar.core.domain.model.EventType
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.HabitConflict
import ru.itmo.dws.calendar.core.domain.model.UpdateHabitRequest
import ru.itmo.dws.calendar.core.domain.model.toHabitConflict
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.input.HabitCreationResult
import ru.itmo.dws.calendar.core.port.input.HabitCreationStatus
import ru.itmo.dws.calendar.core.port.input.HabitManagementUseCase
import ru.itmo.dws.calendar.core.port.output.HabitRepository
import ru.itmo.dws.calendar.core.service.provider.SchedulableEventProvider

class HabitManagementService(
    private val habitRepository: HabitRepository,
    private val eventProviders: List<SchedulableEventProvider>,
    private val habitSlotFinder: HabitSlotFinder,
    private val conflictDetectionService: ConflictDetectionService,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : HabitManagementUseCase {

    override fun createHabit(request: CreateHabitRequest): HabitCreationResult {
        val habit = request.toHabit()
        val today = LocalDate.now(zoneId)

        val scheduledSlot = if (habit.shouldOccurOn(today)) {
            findSlotForDate(habit, today, request.preferredStartTime)
        } else {
            null
        }

        val habitWithSlot = if (scheduledSlot != null) {
            habit.copy(currentTimeSlot = scheduledSlot)
        } else {
            habit
        }

        habitRepository.saveHabit(habitWithSlot)

        val conflicts = if (scheduledSlot != null) {
            detectConflictsForHabit(habitWithSlot, today)
        } else {
            emptyList()
        }

        val status = when {
            scheduledSlot != null && conflicts.isEmpty() -> HabitCreationStatus.CREATED_WITH_SLOT
            scheduledSlot != null -> HabitCreationStatus.CREATED_WITH_CONFLICTS
            else -> HabitCreationStatus.CREATED_WITHOUT_SLOT
        }

        return HabitCreationResult(
            habit = habitWithSlot,
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

        return updatedHabit
    }

    override fun deleteHabit(habitId: HabitId) {
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

    private fun findSlotForDate(
        habit: Habit,
        date: LocalDate,
        preferredStartTime: java.time.LocalTime?
    ): TimeSlot? {
        val occupiedSlots = collectOccupiedSlotsForUser(habit.userId, date, habit.id.toString())

        return habitSlotFinder.findOptimalSlot(
            duration = habit.duration,
            flexibilityWindow = habit.flexibilityWindow,
            date = date,
            occupiedSlots = occupiedSlots,
            bufferTime = habit.bufferTime,
            preferredStartTime = preferredStartTime,
            zoneId = zoneId
        )
    }

    private fun collectOccupiedSlotsForUser(
        userId: UserId,
        date: LocalDate,
        excludeEventId: String? = null
    ): List<TimeSlot> {
        val allEvents = eventProviders.flatMap { provider ->
            provider.getEventsForUserOnDate(userId, date)
        }
        return habitSlotFinder.collectOccupiedSlots(allEvents, excludeEventId)
    }

    private fun detectConflictsForHabit(habit: Habit, date: LocalDate): List<HabitConflict> {
        return conflictDetectionService.detectAllConflictsForUser(habit.userId, date)
            .filter { it.sourceEvent.eventType == EventType.HABIT && it.sourceEvent.eventId == habit.id.toString() }
            .mapNotNull { it.toHabitConflict() }
    }
}
