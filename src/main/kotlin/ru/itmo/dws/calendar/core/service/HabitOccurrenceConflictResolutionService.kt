package ru.itmo.dws.calendar.core.service

import java.time.LocalDate
import java.time.ZoneId
import org.slf4j.LoggerFactory
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.model.OccurrenceStatus
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.HabitOccurrenceRepository
import ru.itmo.dws.calendar.core.port.output.HabitRepository
import ru.itmo.dws.calendar.core.service.provider.SchedulableEventProvider

class HabitOccurrenceConflictResolutionService(
    private val habitOccurrenceRepository: HabitOccurrenceRepository,
    private val habitRepository: HabitRepository,
    private val conflictDetectionService: ConflictDetectionService,
    private val eventSlotFinder: EventSlotFinder,
    private val habitSyncService: HabitSyncService,
    private val eventProviders: List<SchedulableEventProvider>,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    private val log = LoggerFactory.getLogger(HabitOccurrenceConflictResolutionService::class.java)

    fun resolveConflictsForPeriod(days: Int): ResolutionResult {
        val today = LocalDate.now(zoneId)
        val endDate = today.plusDays(days.toLong())

        log.info("Starting conflict resolution for period: {} to {}", today, endDate)

        val allHabits = habitRepository.findAllHabits()
        var resolvedCount = 0
        var unresolvedCount = 0
        var movedToNextDayCount = 0

        for (habit in allHabits) {
            val occurrences = habitOccurrenceRepository.findByHabitIdAndDateRange(
                habit.id,
                today,
                endDate
            ).filter { it.status == OccurrenceStatus.SCHEDULED && it.timeSlot != null }

            for (occurrence in occurrences) {
                val result = resolveConflictForOccurrence(habit, occurrence)
                when (result) {
                    is OccurrenceResolutionResult.Resolved -> resolvedCount++
                    is OccurrenceResolutionResult.MovedToNextDay -> movedToNextDayCount++
                    is OccurrenceResolutionResult.Unresolved -> unresolvedCount++
                    is OccurrenceResolutionResult.NoConflict -> {} // Ничего не делаем
                }
            }
        }

        log.info(
            "Conflict resolution completed: resolved={}, movedToNextDay={}, unresolved={}",
            resolvedCount,
            movedToNextDayCount,
            unresolvedCount
        )

        return ResolutionResult(
            resolvedCount = resolvedCount,
            movedToNextDayCount = movedToNextDayCount,
            unresolvedCount = unresolvedCount
        )
    }

    private fun resolveConflictForOccurrence(
        habit: Habit,
        occurrence: HabitOccurrence
    ): OccurrenceResolutionResult {
        val timeSlot = occurrence.timeSlot ?: return OccurrenceResolutionResult.NoConflict

        val conflicts = conflictDetectionService.detectConflictsInTimeSlot(
            userId = habit.userId,
            timeSlot = timeSlot,
            excludeEventId = occurrence.habitId.toString()
        )

        if (conflicts.isEmpty()) {
            return OccurrenceResolutionResult.NoConflict
        }

        log.info(
            "Detected {} conflict(s) for habit {} on {}: {}",
            conflicts.size,
            habit.id,
            occurrence.date,
            timeSlot
        )

        val sameDaySlot = findFreeSlotSameDay(habit, occurrence.date, habit.userId)
        if (sameDaySlot != null) {
            return rescheduleOccurrence(habit, occurrence, sameDaySlot, "Rescheduled to avoid conflict")
        }

        if (habit.flexibilityWindow.allowCrossDayMove) {
            val nextDayResult = tryMoveToNextDay(habit, occurrence)
            if (nextDayResult != null) {
                return nextDayResult
            }
        }

        return markAsUnresolved(occurrence, "Conflict detected, no free slot available")
    }

    private fun findFreeSlotSameDay(
        habit: Habit,
        date: LocalDate,
        userId: UserId
    ): TimeSlot? {
        val occupiedSlots = collectOccupiedSlotsForDate(userId, date, habit.id.toString())

        return eventSlotFinder.findOptimalSlot(
            event = habit,
            date = date,
            baseTimeWindow = habit.flexibilityTimeRange(),
            eventDuration = habit.duration,
            occupiedSlots = occupiedSlots,
            bufferTime = habit.bufferTime,
            preferredStartTime = habit.preferredStartTime(),
            zoneId = zoneId
        )
    }

    private fun tryMoveToNextDay(
        habit: Habit,
        occurrence: HabitOccurrence,
        maxDaysAhead: Int = 3
    ): OccurrenceResolutionResult? {
        for (daysOffset in 1..maxDaysAhead) {
            val nextDate = occurrence.date.plusDays(daysOffset.toLong())

            if (!habit.shouldOccurOn(nextDate)) {
                continue
            }

            val existingOccurrence = habitOccurrenceRepository.findByHabitIdAndDate(habit.id, nextDate)
            if (existingOccurrence != null) {
                continue
            }

            val nextDaySlot = findFreeSlotSameDay(habit, nextDate, habit.userId)
            if (nextDaySlot != null) {
                habitOccurrenceRepository.delete(occurrence)

                val newOccurrence = HabitOccurrence(
                    habitId = habit.id,
                    date = nextDate,
                    status = OccurrenceStatus.SCHEDULED,
                    timeSlot = nextDaySlot
                )

                val saved = habitOccurrenceRepository.save(newOccurrence)
                habitSyncService.syncSingleOccurrence(habit, saved)

                log.info(
                    "Moved habit {} from {} to {} (slot: {})",
                    habit.id,
                    occurrence.date,
                    nextDate,
                    nextDaySlot
                )

                return OccurrenceResolutionResult.MovedToNextDay(
                    originalDate = occurrence.date,
                    newDate = nextDate,
                    newTimeSlot = nextDaySlot
                )
            }
        }

        return null
    }

    private fun rescheduleOccurrence(
        habit: Habit,
        occurrence: HabitOccurrence,
        newTimeSlot: TimeSlot,
        reason: String
    ): OccurrenceResolutionResult {
        val updated = occurrence.copy(
            timeSlot = newTimeSlot,
            status = OccurrenceStatus.SCHEDULED,
            reason = null
        )

        habitOccurrenceRepository.update(updated)
        habitSyncService.syncSingleOccurrence(habit, updated)

        log.info(
            "Rescheduled habit {} on {} from {} to {}",
            habit.id,
            occurrence.date,
            occurrence.timeSlot,
            newTimeSlot
        )

        return OccurrenceResolutionResult.Resolved(
            originalTimeSlot = occurrence.timeSlot!!,
            newTimeSlot = newTimeSlot,
            reason = reason
        )
    }

    private fun markAsUnresolved(
        occurrence: HabitOccurrence,
        reason: String
    ): OccurrenceResolutionResult {
        val updated = occurrence.copy(
            status = OccurrenceStatus.CONFLICT_UNRESOLVED,
            reason = reason
        )

        habitOccurrenceRepository.update(updated)

        log.warn(
            "Could not resolve conflict for habit {} on {}: {}",
            occurrence.habitId,
            occurrence.date,
            reason
        )

        return OccurrenceResolutionResult.Unresolved(reason)
    }

    private fun collectOccupiedSlotsForDate(
        userId: UserId,
        date: LocalDate,
        excludeEventId: String
    ): List<TimeSlot> {
        val allEvents = eventProviders.flatMap { provider ->
            provider.getEventsForUserOnDate(userId, date)
        }
        return eventSlotFinder.collectOccupiedSlots(allEvents, excludeEventId)
    }
}

data class ResolutionResult(
    val resolvedCount: Int,
    val movedToNextDayCount: Int,
    val unresolvedCount: Int
) {
    val totalProcessed: Int get() = resolvedCount + movedToNextDayCount + unresolvedCount
    val isFullySuccessful: Boolean get() = unresolvedCount == 0
}

sealed class OccurrenceResolutionResult {
    data object NoConflict : OccurrenceResolutionResult()

    data class Resolved(
        val originalTimeSlot: TimeSlot,
        val newTimeSlot: TimeSlot,
        val reason: String
    ) : OccurrenceResolutionResult()

    data class MovedToNextDay(
        val originalDate: LocalDate,
        val newDate: LocalDate,
        val newTimeSlot: TimeSlot
    ) : OccurrenceResolutionResult()

    data class Unresolved(
        val reason: String
    ) : OccurrenceResolutionResult()
}
