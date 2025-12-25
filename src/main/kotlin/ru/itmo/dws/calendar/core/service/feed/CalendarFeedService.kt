package ru.itmo.dws.calendar.core.service.feed

import org.slf4j.LoggerFactory
import ru.itmo.dws.calendar.core.domain.model.CalendarEvent
import ru.itmo.dws.calendar.core.domain.model.CalendarFeedItem
import ru.itmo.dws.calendar.core.domain.model.CalendarItemType
import ru.itmo.dws.calendar.core.domain.model.ConflictInfo
import ru.itmo.dws.calendar.core.domain.model.ConflictType
import ru.itmo.dws.calendar.core.domain.model.EventSource
import ru.itmo.dws.calendar.core.domain.model.ItemCapabilities
import ru.itmo.dws.calendar.core.domain.model.ItemDetails
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.input.CalendarFeedResult
import ru.itmo.dws.calendar.core.port.input.CalendarFeedUseCase
import ru.itmo.dws.calendar.core.port.output.CalendarProvider
import ru.itmo.dws.calendar.core.port.output.InternalEventProvider

class CalendarFeedService(
    private val calendarProvider: CalendarProvider?,
    private val eventProviders: List<InternalEventProvider>
) : CalendarFeedUseCase {
    private val log = LoggerFactory.getLogger(CalendarFeedService::class.java)

    override fun getCalendarFeed(userId: UserId, timeRange: TimeSlot): CalendarFeedResult {
        val internalEvents = collectInternalEvents(userId, timeRange)

        val externalEvents = getExternalEvents(userId, timeRange)

        val mergedEvents = mergeEvents(internalEvents, externalEvents)

        val eventsWithConflicts = detectConflicts(mergedEvents)

        val sortedEvents = eventsWithConflicts.sortedBy { it.timeSlot.start }

        return CalendarFeedResult.of(sortedEvents, timeRange)
    }

    private fun collectInternalEvents(userId: UserId, timeRange: TimeSlot): List<CalendarFeedItem> {
        return eventProviders.flatMap { provider ->
            try {
                provider.getEvents(userId, timeRange)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                log.warn("Failed to get events from provider {}: {}", provider.eventType(), e.message)
                emptyList()
            }
        }
    }

    private fun getExternalEvents(userId: UserId, timeRange: TimeSlot): List<CalendarEvent> {
        if (calendarProvider == null) {
            return emptyList()
        }

        return try {
            calendarProvider.getEvents(userId, timeRange)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            log.warn("Failed to get external events for user {}: {}", userId, e.message)
            emptyList()
        }
    }

    private fun mergeEvents(
        internalEvents: List<CalendarFeedItem>,
        externalEvents: List<CalendarEvent>
    ): List<CalendarFeedItem> {
        val mirroredExternalIds = internalEvents
            .mapNotNull { it.externalEventId }
            .toSet()

        val unmirroredExternalEvents = externalEvents
            .filter { it.externalId !in mirroredExternalIds }
            .map { toExternalFeedItem(it) }

        return internalEvents + unmirroredExternalEvents
    }

    private fun toExternalFeedItem(event: CalendarEvent): CalendarFeedItem {
        return CalendarFeedItem(
            id = "ext_${event.externalId}",
            timeSlot = event.timeSlot,
            title = event.title,
            description = event.description,
            itemType = CalendarItemType.EXTERNAL,
            source = EventSource.EXTERNAL_ONLY,
            externalEventId = event.externalId,
            capabilities = ItemCapabilities.forExternalEvent(),
            conflict = null,
            details = ItemDetails.External(
                calendarId = event.calendarId,
                isAllDay = event.isAllDay
            )
        )
    }

    private fun detectConflicts(events: List<CalendarFeedItem>): List<CalendarFeedItem> {
        if (events.size < 2) return events

        val sortedEvents = events.sortedBy { it.timeSlot.start }

        return sortedEvents.map { current ->
            val conflictingIds = findConflictingEventIds(current, sortedEvents)
            attachConflictInfo(current, conflictingIds)
        }
    }

    private fun findConflictingEventIds(
        current: CalendarFeedItem,
        allEvents: List<CalendarFeedItem>
    ): List<String> {
        return allEvents
            .filter { other -> other.id != current.id }
            .filter { other -> current.timeSlot.overlapsWith(other.timeSlot) }
            .map { it.id }
    }

    private fun attachConflictInfo(
        event: CalendarFeedItem,
        conflictingIds: List<String>
    ): CalendarFeedItem {
        if (conflictingIds.isEmpty()) return event

        return event.copy(
            conflict = ConflictInfo(
                conflictType = ConflictType.TIME_OVERLAP,
                conflictingEventIds = conflictingIds
            )
        )
    }
}
