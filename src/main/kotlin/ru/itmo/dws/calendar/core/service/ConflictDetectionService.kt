package ru.itmo.dws.calendar.core.service

import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.model.EventConflict
import ru.itmo.dws.calendar.core.domain.model.EventType
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.service.provider.SchedulableEventProvider

class ConflictDetectionService(
    private val eventProviders: List<SchedulableEventProvider>
) {

    fun detectAllConflictsForUser(userId: UserId, date: LocalDate): List<EventConflict> {
        val allEvents = collectAllEventsForUser(userId, date)
        return EventConflict.detectAll(allEvents, date)
    }

    fun detectConflictsForEvent(
        event: SchedulableEvent,
        date: LocalDate
    ): List<EventConflict> {
        val userId = event.affectedUsers.firstOrNull() ?: return emptyList()
        val allEvents = collectAllEventsForUser(userId, date)
            .filter { it.eventId != event.eventId }

        return allEvents.mapNotNull { other ->
            EventConflict.between(event, other, date)
        }
    }

    fun detectConflictsInTimeSlot(
        userId: UserId,
        timeSlot: TimeSlot,
        excludeEventId: String? = null
    ): List<EventConflict> {
        val date = timeSlot.start.toLocalDate()
        val eventsInSlot = eventProviders
            .flatMap { it.getEventsForUser(userId, timeSlot) }
            .filter { excludeEventId == null || it.eventId != excludeEventId }

        return EventConflict.detectAll(eventsInSlot, date)
    }

    fun detectConflictsBetweenTypes(
        userId: UserId,
        date: LocalDate,
        sourceType: EventType,
        targetType: EventType
    ): List<EventConflict> {
        val sourceProvider = eventProviders.find { it.eventType == sourceType }
        val targetProvider = eventProviders.find { it.eventType == targetType }

        if (sourceProvider == null || targetProvider == null) {
            return emptyList()
        }

        val sourceEvents = sourceProvider.getEventsForUserOnDate(userId, date)
        val targetEvents = targetProvider.getEventsForUserOnDate(userId, date)

        return sourceEvents.flatMap { source ->
            targetEvents.mapNotNull { target ->
                EventConflict.between(source, target, date)
            }
        }
    }

    fun hasConflicts(event: SchedulableEvent, date: LocalDate): Boolean {
        return detectConflictsForEvent(event, date).isNotEmpty()
    }

    private fun collectAllEventsForUser(userId: UserId, date: LocalDate): List<SchedulableEvent> {
        return eventProviders.flatMap { provider ->
            provider.getEventsForUserOnDate(userId, date)
        }
    }
}
