package ru.itmo.dws.calendar.infrastructure.calendar

import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Events
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.core.domain.exception.ExternalCalendarSyncException
import ru.itmo.dws.calendar.core.domain.model.CalendarEvent
import ru.itmo.dws.calendar.core.domain.model.OccurrenceEvent
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.valueobject.CalendarId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.CalendarProvider
import ru.itmo.dws.calendar.dto.google.CreateEventRequest
import ru.itmo.dws.calendar.dto.google.EventDateTime
import ru.itmo.dws.calendar.provider.GoogleCalendarProvider
import ru.itmo.dws.calendar.repository.UserRepository

@Component
@Suppress("TooManyFunctions")
class GoogleCalendarProviderAdapter(
    private val googleCalendarProvider: GoogleCalendarProvider,
    private val gsonFactory: GsonFactory,
    private val userRepository: UserRepository
) : CalendarProvider {

    private val log = LoggerFactory.getLogger(GoogleCalendarProviderAdapter::class.java)

    override fun getEvents(userId: UserId, timeRange: TimeSlot): List<CalendarEvent> {
        var username = getCurrentUsername()

        if (username == null) {
            username = getUsernameByUserId(userId)
            if (username == null) {
                log.error("User not found in DB for userId={}", userId)
                return emptyList()
            }
        }

        return try {
            val eventsJson = googleCalendarProvider.getEventsByCalendarId(
                username,
                PRIMARY_CALENDAR,
                timeMin = timeRange.start,
                timeMax = timeRange.end
            )
            parseEventsFromJson(eventsJson, userId)
        } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
            log.error("Failed to get events for user {}: {}", userId, e.message, e)
            emptyList()
        }
    }
    
    private fun getUsernameByUserId(userId: UserId): String? {
        return try {
            val user = userRepository.findById(userId.value)
            user?.username
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            log.error("Error fetching user from DB: {}", e.message, e)
            null
        }
    }

    private fun getUsernameOrThrow(userId: UserId, operation: String): String {
        var username = getCurrentUsername()

        if (username == null) {
            log.debug("No SecurityContext for {}, fetching username from DB for userId={}", operation, userId)
            username = getUsernameByUserId(userId)
            if (username == null) {
                throw IllegalStateException(
                    "Cannot $operation: user not found for userId=$userId. " +
                    "User must exist in database and have a valid login/email."
                )
            }
        }
        
        return username
    }

    override fun getEventsForUsers(
        userIds: List<UserId>,
        timeRange: TimeSlot
    ): Map<UserId, List<CalendarEvent>> {
        return userIds.associateWith { getEvents(it, timeRange) }
    }

    override fun createEvent(userId: UserId, event: SchedulableEvent): String {
        val username = getUsernameOrThrow(userId, "create event")
        val request = toCreateEventRequest(event)

        val createdEvent = googleCalendarProvider.createEvent(username, PRIMARY_CALENDAR, request)
            ?: throw ExternalCalendarSyncException("Failed to create event in external calendar")

        log.info("Created event {} in external calendar for user {}", createdEvent.id, userId)
        return createdEvent.id
    }

    override fun updateEvent(userId: UserId, externalEventId: String, event: SchedulableEvent): Boolean {
        val username = getUsernameOrThrow(userId, "update event")
        val request = toCreateEventRequest(event)

        val updatedEvent = googleCalendarProvider.patchEvent(username, PRIMARY_CALENDAR, externalEventId, request)

        return if (updatedEvent != null) {
            log.info("Updated event {} in external calendar for user {}", externalEventId, userId)
            true
        } else {
            log.warn("Failed to update event {} in external calendar", externalEventId)
            false
        }
    }

    override fun deleteEvent(userId: UserId, externalEventId: String): Boolean {
        val username = getUsernameOrThrow(userId, "delete event")

        return try {
            googleCalendarProvider.deleteEventById(username, PRIMARY_CALENDAR, externalEventId)
            log.info("Deleted event {} from external calendar for user {}", externalEventId, userId)
            true
        } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
            log.warn("Failed to delete event {}: {}", externalEventId, e.message)
            false
        }
    }

    private fun toCreateEventRequest(event: SchedulableEvent): CreateEventRequest {
        val timeSlot = event.effectiveTimeSlot()
            ?: throw IllegalArgumentException("Event must have a time slot")

        val description = buildEventDescription(event)

        return CreateEventRequest(
            start = toEventDateTime(timeSlot.start),
            end = toEventDateTime(timeSlot.end),
            summary = event.title,
            description = description,
            eventType = SMART_CALENDAR_EVENT_TYPE,
            anyoneCanAddSelf = null,
            attachments = null,
            attendees = null,
            birthdayProperties = null,
            colorId = null,
            gadget = null,
            guestsCanInviteOthers = null,
            guestsCanModify = null,
            guestsCanSeeOtherGuests = null,
            id = null,
            location = null,
            originationStartTime = null,
            recurrence = null,
            reminders = null,
            sequence = null,
            source = null,
            status = null,
            transparency = null,
            visibility = null,
            workingLocationProperties = null
        )
    }

    private fun toEventDateTime(zonedDateTime: ZonedDateTime): EventDateTime {
        val googleDateTime = DateTime(zonedDateTime.toInstant().toEpochMilli())
        return EventDateTime(
            date = null,
            dateTime = googleDateTime,
            timeZone = zonedDateTime.zone.id
        )
    }

    private fun buildEventDescription(event: SchedulableEvent): String {
        val baseDescription = event.description ?: ""

        return if (event is OccurrenceEvent) {
            """
            |$baseDescription
            |
            |---
            |[SmartCalendar Metadata]
            |habitId: ${event.habitId}
            |occurrenceDate: ${event.occurrenceDate}
            |source: $SOURCE_APP
            """.trimMargin()
        } else {
            """
            |$baseDescription
            |
            |---
            |[SmartCalendar Metadata]
            |eventId: ${event.eventId}
            |eventType: ${event.eventType}
            |source: $SOURCE_APP
            """.trimMargin()
        }
    }

    private fun parseEventsFromJson(json: String, userId: UserId): List<CalendarEvent> {
        if (json.isBlank()) {
            return emptyList()
        }

        return try {
            val events = gsonFactory.fromString(json, Events::class.java)
            val allItems = events.items ?: emptyList()
            val nonSmartCalendarEvents = allItems.filter { !isSmartCalendarEvent(it.description) }
            
            nonSmartCalendarEvents.mapNotNull { googleEvent -> 
                mapToCalendarEvent(googleEvent, userId) 
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            log.error("Failed to parse events JSON: {}", e.message, e)
            emptyList()
        }
    }

    private fun isSmartCalendarEvent(description: String?): Boolean {
        return description?.contains(METADATA_MARKER) == true
    }

    private fun mapToCalendarEvent(
        googleEvent: com.google.api.services.calendar.model.Event,
        userId: UserId
    ): CalendarEvent? {
        val startDateTime = googleEvent.start?.dateTime ?: googleEvent.start?.date
        val endDateTime = googleEvent.end?.dateTime ?: googleEvent.end?.date

        if (startDateTime == null || endDateTime == null) {
            log.debug("Skipping event {} - missing start/end time", googleEvent.id)
            return null
        }

        val timeZone = googleEvent.start?.timeZone?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()

        val startZoned = toZonedDateTime(startDateTime, timeZone)
        val endZoned = toZonedDateTime(endDateTime, timeZone)

        if (startZoned == null || endZoned == null) {
            log.debug("Skipping event {} - failed to parse datetime", googleEvent.id)
            return null
        }

        val isAllDay = googleEvent.start?.date != null

        return CalendarEvent(
            externalId = googleEvent.id,
            calendarId = CalendarId(PRIMARY_CALENDAR),
            owner = userId,
            timeSlot = TimeSlot(startZoned, endZoned),
            title = googleEvent.summary ?: "Untitled",
            description = googleEvent.description,
            participants = emptyList(),
            eventType = CalendarEvent.EventType.REGULAR,
            isAllDay = isAllDay
        )
    }

    private fun toZonedDateTime(dateTime: DateTime, zoneId: ZoneId): ZonedDateTime? {
        return try {
            val instant = Instant.ofEpochMilli(dateTime.value)
            ZonedDateTime.ofInstant(instant, zoneId)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            log.debug("Failed to parse datetime {}: {}", dateTime, e.message)
            null
        }
    }

    private fun getCurrentUsername(): String? {
        return SecurityContextHolder.getContext().authentication?.name
    }

    companion object {
        const val PRIMARY_CALENDAR = "primary"
        const val SMART_CALENDAR_EVENT_TYPE = "default"
        const val SOURCE_APP = "smart-calendar"
        const val METADATA_MARKER = "[SmartCalendar Metadata]"
    }
}
