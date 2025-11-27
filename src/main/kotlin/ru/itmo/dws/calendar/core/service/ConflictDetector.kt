package ru.itmo.dws.calendar.core.service

import java.time.LocalDate
import java.time.ZoneId
import ru.itmo.dws.calendar.core.domain.model.EventConflict
import ru.itmo.dws.calendar.core.domain.model.EventType
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

class ConflictDetector(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun detectConflictsFor(
        event: SchedulableEvent,
        otherEvents: List<SchedulableEvent>,
        date: LocalDate
    ): List<EventConflict> {
        return otherEvents
            .filter { it.eventId != event.eventId }
            .mapNotNull { other -> EventConflict.between(event, other, date) }
    }

    fun detectConflictsForUser(
        userId: UserId,
        events: List<SchedulableEvent>,
        date: LocalDate
    ): List<EventConflict> {
        val userEvents = events.filter { it.affectsUser(userId) }
        return detectAllConflicts(userEvents, date)
    }

    fun detectConflictsByType(
        sourceType: EventType,
        targetType: EventType,
        events: List<SchedulableEvent>,
        date: LocalDate
    ): List<EventConflict> {
        val sourceEvents = events.filter { it.eventType == sourceType }
        val targetEvents = events.filter { it.eventType == targetType }

        val conflicts = mutableListOf<EventConflict>()
        for (source in sourceEvents) {
            for (target in targetEvents) {
                if (source.eventId != target.eventId) {
                    EventConflict.between(source, target, date)?.let { conflicts.add(it) }
                }
            }
        }
        return conflicts
    }

    fun hasConflicts(
        event: SchedulableEvent,
        otherEvents: List<SchedulableEvent>,
        date: LocalDate
    ): Boolean {
        return detectConflictsFor(event, otherEvents, date).isNotEmpty()
    }

    fun findConflictingEvents(
        timeSlot: TimeSlot,
        events: List<SchedulableEvent>
    ): List<SchedulableEvent> {
        return events.filter { event ->
            event.effectiveTimeSlot()?.overlapsWith(timeSlot) == true
        }
    }

    fun createDayTimeSlot(date: LocalDate): TimeSlot {
        val startOfDay = date.atStartOfDay(zoneId)
        val endOfDay = date.plusDays(1).atStartOfDay(zoneId)
        return TimeSlot(startOfDay, endOfDay)
    }

    private fun detectAllConflicts(
        events: List<SchedulableEvent>,
        date: LocalDate
    ): List<EventConflict> {
        return EventConflict.detectAll(events, date)
    }
}
