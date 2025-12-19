package ru.itmo.dws.calendar.core.service

import org.slf4j.LoggerFactory
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.model.OccurrenceEvent
import ru.itmo.dws.calendar.core.domain.model.OccurrenceStatus
import ru.itmo.dws.calendar.core.port.output.CalendarProvider
import ru.itmo.dws.calendar.core.port.output.HabitOccurrenceRepository

class HabitSyncService(
    private val occurrenceRepository: HabitOccurrenceRepository,
    private val calendarProvider: CalendarProvider? = null
) {
    private val log = LoggerFactory.getLogger(HabitSyncService::class.java)

    fun syncOccurrencesToExternalCalendar(habit: Habit, occurrences: List<HabitOccurrence>): SyncResult {
        if (calendarProvider == null) {
            log.debug("No calendar provider, saving occurrences locally only for habit {}", habit.id)
            val savedOccurrences = saveOccurrencesLocally(occurrences)
            return SyncResult(
                syncedCount = 0,
                failedCount = 0,
                skippedCount = savedOccurrences.size,
                occurrences = savedOccurrences
            )
        }

        val scheduledOccurrences = occurrences.filter { it.isScheduled && it.timeSlot != null }
        val unscheduledOccurrences = occurrences.filter { !it.isScheduled }

        val savedUnscheduled = saveOccurrencesLocally(unscheduledOccurrences)

        var syncedCount = 0
        var failedCount = 0
        val syncedOccurrences = mutableListOf<HabitOccurrence>()

        for (occurrence in scheduledOccurrences) {
            val syncedOccurrence = syncSingleOccurrence(habit, occurrence)
            if (syncedOccurrence.isSynced) {
                syncedCount++
            } else {
                failedCount++
            }
            syncedOccurrences.add(syncedOccurrence)
        }

        val allOccurrences = syncedOccurrences + savedUnscheduled

        return SyncResult(
            syncedCount = syncedCount,
            failedCount = failedCount,
            skippedCount = savedUnscheduled.size,
            occurrences = allOccurrences
        )
    }

    fun syncSingleOccurrence(habit: Habit, occurrence: HabitOccurrence): HabitOccurrence {
        if (calendarProvider == null || !occurrence.isScheduled || occurrence.timeSlot == null) {
            return occurrenceRepository.save(occurrence)
        }

        if (occurrence.isSynced) {
            return updateExternalEvent(habit, occurrence)
        }

        return createExternalEvent(habit, occurrence)
    }

    fun deleteOccurrenceFromExternalCalendar(habit: Habit, occurrence: HabitOccurrence): Boolean {
        if (calendarProvider == null || occurrence.externalEventId == null) {
            return true
        }

        return try {
            calendarProvider.deleteEvent(habit.userId, occurrence.externalEventId)
            log.info(
                "Deleted external event {} for habit {} on {}",
                occurrence.externalEventId,
                habit.id,
                occurrence.date
            )
            true
        } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
            log.warn(
                "Failed to delete external event {} for habit {}: {}",
                occurrence.externalEventId,
                habit.id,
                e.message
            )
            false
        }
    }

    fun deleteAllOccurrencesFromExternalCalendar(habit: Habit): Int {
        val occurrences = occurrenceRepository.findByHabitId(habit.id)
        var deletedCount = 0

        for (occurrence in occurrences) {
            if (deleteOccurrenceFromExternalCalendar(habit, occurrence)) {
                deletedCount++
            }
        }

        occurrenceRepository.deleteByHabitId(habit.id)
        return deletedCount
    }

    private fun createExternalEvent(habit: Habit, occurrence: HabitOccurrence): HabitOccurrence {
        return try {
            val occurrenceEvent = OccurrenceEvent(habit, occurrence)
            val externalEventId = calendarProvider!!.createEvent(habit.userId, occurrenceEvent)

            log.info(
                "Created external event {} for habit {} on {}",
                externalEventId,
                habit.id,
                occurrence.date
            )

            val syncedOccurrence = occurrence.copy(
                externalEventId = externalEventId,
                status = OccurrenceStatus.SCHEDULED
            )
            occurrenceRepository.save(syncedOccurrence)
        } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
            log.warn(
                "Failed to create external event for habit {} on {}: {}",
                habit.id,
                occurrence.date,
                e.message
            )
            occurrenceRepository.save(occurrence)
        }
    }

    private fun updateExternalEvent(habit: Habit, occurrence: HabitOccurrence): HabitOccurrence {
        return try {
            val occurrenceEvent = OccurrenceEvent(habit, occurrence)
            calendarProvider!!.updateEvent(
                habit.userId,
                occurrence.externalEventId!!,
                occurrenceEvent
            )

            log.info(
                "Updated external event {} for habit {} on {}",
                occurrence.externalEventId,
                habit.id,
                occurrence.date
            )

            occurrenceRepository.save(occurrence)
        } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
            log.warn(
                "Failed to update external event for habit {} on {}: {}",
                habit.id,
                occurrence.date,
                e.message
            )
            occurrenceRepository.save(occurrence)
        }
    }

    private fun saveOccurrencesLocally(occurrences: List<HabitOccurrence>): List<HabitOccurrence> {
        return occurrences.map { occurrenceRepository.save(it) }
    }
}

data class SyncResult(
    val syncedCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val occurrences: List<HabitOccurrence>
) {
    val totalCount: Int get() = syncedCount + failedCount + skippedCount
    val isFullySuccessful: Boolean get() = failedCount == 0
}
