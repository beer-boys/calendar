package ru.itmo.dws.calendar.core.domain.model

import java.time.Instant
import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

data class EventConflict(
    val sourceEvent: SchedulableEvent,
    val conflictingEvent: SchedulableEvent,
    val overlapTimeSlot: TimeSlot,
    val affectedUsers: Set<UserId>,
    val affectedDate: LocalDate,
    val detectedAt: Instant = Instant.now()
) {
    companion object {

        fun between(
            source: SchedulableEvent,
            other: SchedulableEvent,
            date: LocalDate
        ): EventConflict? {
            val sourceSlot = source.effectiveTimeSlot() ?: return null
            val otherSlot = other.effectiveTimeSlot() ?: return null

            if (!sourceSlot.overlapsWith(otherSlot)) return null
            if (!source.hasCommonUsers(other)) return null

            val overlapStart = maxOf(sourceSlot.start, otherSlot.start)
            val overlapEnd = minOf(sourceSlot.end, otherSlot.end)

            return EventConflict(
                sourceEvent = source,
                conflictingEvent = other,
                overlapTimeSlot = TimeSlot(overlapStart, overlapEnd),
                affectedUsers = (source.affectedUsers + other.affectedUsers).toSet(),
                affectedDate = date
            )
        }

        fun detectAll(events: List<SchedulableEvent>, date: LocalDate): List<EventConflict> {
            val conflicts = mutableListOf<EventConflict>()

            for (i in events.indices) {
                for (j in i + 1 until events.size) {
                    between(events[i], events[j], date)?.let { conflicts.add(it) }
                }
            }

            return conflicts
        }
    }
}

fun EventConflict.toHabitConflict(): HabitConflict? {
    val habit = when (sourceEvent) {
        is Habit -> sourceEvent
        else -> return null
    }

    val conflictingEventWrapper = when (conflictingEvent) {
        is Meeting -> ConflictingEvent.MeetingEvent(conflictingEvent)
        is FocusTime -> ConflictingEvent.FocusTimeEvent(conflictingEvent)
        is Habit -> ConflictingEvent.HabitEvent(conflictingEvent)
        else -> return null
    }

    val conflictType = when (conflictingEvent.eventType) {
        EventType.MEETING, EventType.EXTERNAL_EVENT -> HabitConflict.ConflictType.MEETING_OVERLAP
        EventType.FOCUS_TIME -> HabitConflict.ConflictType.FOCUS_TIME_OVERLAP
        EventType.HABIT -> HabitConflict.ConflictType.HABIT_OVERLAP
    }

    return HabitConflict(
        habitId = habit.id,
        habitTitle = habit.title,
        habitTimeSlot = habit.effectiveTimeSlot()!!,
        habitPriority = habit.priority,
        conflictingEvent = conflictingEventWrapper,
        conflictType = conflictType,
        affectedDate = affectedDate,
        userId = habit.userId,
        detectedAt = detectedAt
    )
}
