package ru.itmo.dws.calendar.core.service

import java.time.LocalDate
import java.time.LocalTime
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
import ru.itmo.dws.calendar.core.port.output.CalendarProvider
import ru.itmo.dws.calendar.core.port.output.HabitOccurrenceRepository
import ru.itmo.dws.calendar.core.port.output.HabitRepository
import ru.itmo.dws.calendar.core.service.provider.SchedulableEventProvider

@Suppress("LongParameterList")
class HabitManagementService(
    private val habitRepository: HabitRepository,
    private val occurrenceRepository: HabitOccurrenceRepository,
    private val eventProviders: List<SchedulableEventProvider>,
    private val eventSlotFinder: EventSlotFinder,
    private val conflictDetectionService: ConflictDetectionService,
    private val habitSchedulingService: HabitSchedulingService,
    private val habitSyncService: HabitSyncService,
    private val calendarProvider: CalendarProvider? = null,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : HabitManagementUseCase {

    private val log = LoggerFactory.getLogger(HabitManagementService::class.java)

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

        val habitWithExternalId = syncToExternalCalendar(habitWithSlot)

        habitRepository.saveHabit(habitWithExternalId)

        val conflicts = if (scheduledSlot != null) {
            detectConflictsForHabit(habitWithExternalId, today)
        } else {
            emptyList()
        }

        val status = when {
            scheduledSlot != null && conflicts.isEmpty() -> HabitCreationStatus.CREATED_WITH_SLOT
            scheduledSlot != null -> HabitCreationStatus.CREATED_WITH_CONFLICTS
            else -> HabitCreationStatus.CREATED_WITHOUT_SLOT
        }

        return HabitCreationResult(
            habit = habitWithExternalId,
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

        syncUpdateToExternalCalendar(updatedHabit)
        habitRepository.updateHabit(habitId, updatedHabit)

        return updatedHabit
    }

    override fun deleteHabit(habitId: HabitId) {
        val habit = habitRepository.findHabit(habitId)
        if (habit != null) {
            deleteFromExternalCalendar(habit)
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
            habitId, syncResult.syncedCount, syncResult.failedCount, syncResult.skippedCount
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

    private fun findSlotForDate(
        habit: Habit,
        date: LocalDate,
        preferredStartTime: LocalTime?
    ): TimeSlot? {
        val occupiedSlots = collectOccupiedSlotsForUser(habit.userId, date, habit.id.toString())

        return eventSlotFinder.findOptimalSlot(
            event = habit,
            date = date,
            baseTimeWindow = habit.flexibilityTimeRange(),
            eventDuration = habit.duration,
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
        return eventSlotFinder.collectOccupiedSlots(allEvents, excludeEventId)
    }

    private fun detectConflictsForHabit(habit: Habit, date: LocalDate): List<HabitConflict> {
        return conflictDetectionService.detectAllConflictsForUser(habit.userId, date)
            .filter { it.sourceEvent.eventType == EventType.HABIT && it.sourceEvent.eventId == habit.id.toString() }
            .mapNotNull { it.toHabitConflict() }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun syncToExternalCalendar(habit: Habit): Habit {
        if (calendarProvider == null) {
            log.debug("No calendar provider configured, skipping external sync for habit {}", habit.id)
            return habit
        }

        return try {
            val externalEventId = calendarProvider.createRecurringEvent(habit.userId, habit)
            log.info("Created recurring event in external calendar: {} for habit {}", externalEventId, habit.id)
            habit.withExternalEventId(externalEventId)
        } catch (e: RuntimeException) {
            log.warn("Failed to sync habit {} to external calendar: {}", habit.id, e.message)
            habit
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun syncUpdateToExternalCalendar(habit: Habit) {
        if (calendarProvider == null || habit.externalEventId == null) {
            return
        }

        try {
            calendarProvider.updateRecurringEvent(habit.userId, habit.externalEventId, habit)
            log.info("Updated recurring event in external calendar: {} for habit {}", habit.externalEventId, habit.id)
        } catch (e: RuntimeException) {
            log.warn("Failed to update habit {} in external calendar: {}", habit.id, e.message)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun deleteFromExternalCalendar(habit: Habit) {
        if (calendarProvider == null || habit.externalEventId == null) {
            return
        }

        try {
            calendarProvider.deleteEvent(habit.userId, habit.externalEventId)
            log.info("Deleted event from external calendar: {} for habit {}", habit.externalEventId, habit.id)
        } catch (e: RuntimeException) {
            log.warn("Failed to delete habit {} from external calendar: {}", habit.id, e.message)
        }
    }
}
