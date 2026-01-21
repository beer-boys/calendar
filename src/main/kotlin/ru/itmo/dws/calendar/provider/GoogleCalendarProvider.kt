package ru.itmo.dws.calendar.provider

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Colors
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventAttendee
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.core.domain.exception.ExternalEventNotFoundException
import ru.itmo.dws.calendar.dto.google.Attendee
import ru.itmo.dws.calendar.dto.google.CreateEventRequest
import ru.itmo.dws.calendar.dto.google.toGoogleEventDateTime
import ru.itmo.dws.calendar.security.OAuth2Service
import ru.itmo.dws.calendar.service.google.GoogleCalendarFactory

@Component
@Suppress("TooManyFunctions")
class GoogleCalendarProvider(
    private val oAuth2Service: OAuth2Service,
    private val googleCalendarFactory: GoogleCalendarFactory,
) : CalendarProvider {

    companion object {
        private const val PROVIDER_ID = "google"
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun getCalendars(
        username: String,
    ): Map<String, String> {
        val accessToken = oAuth2Service.getAccessToken(username, PROVIDER_ID)
        val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

        val response = calendar.calendarList().list().execute()
        return response.items.associate {
            it.summary to it.id
        }
    }

    override fun getCalendarById(
        username: String,
        calendarId: String
    ): String {
        return try {
            val accessToken = oAuth2Service.getAccessToken(username, PROVIDER_ID)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val response = calendar.calendarList().get(calendarId).execute()
            response.toPrettyString()
        } catch (expected: Exception) {
            log.error("Error while getting calendar $calendarId: ", expected)
            ""
        }
    }

    override fun getEventsByCalendarId(
        username: String,
        calendarId: String,
        timeMin: java.time.ZonedDateTime?,
        timeMax: java.time.ZonedDateTime?
    ): String {
        return try {
            val accessToken = oAuth2Service.getAccessToken(username, PROVIDER_ID)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val request = calendar.events().list(calendarId)

            if (timeMin != null) {
                request.timeMin = DateTime(timeMin.toInstant().toEpochMilli())
            }
            if (timeMax != null) {
                request.timeMax = DateTime(timeMax.toInstant().toEpochMilli())
            }

            val response = request.execute()
            response.toPrettyString()
        } catch (expected: Exception) {
            log.error("Error while getting events for calendarId=$calendarId", expected)
            ""
        }
    }

    override fun getEventByEventIdAndCalendarId(
        username: String,
        calendarId: String,
        eventId: String
    ): String {
        return try {
            val accessToken = oAuth2Service.getAccessToken(username, PROVIDER_ID)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val response = calendar.events().get(calendarId, eventId).execute()
            response.toPrettyString()
        } catch (expected: Exception) {
            log.error("Error while getting events for calendarId=$calendarId", expected)
            ""
        }
    }

    fun quickAdd(
        username: String,
        calendarId: String,
        text: String,
        sendNotifications: Boolean = false,
        sendUpdates: String = "none"
    ): Event? {
        return try {
            val accessToken = oAuth2Service.getAccessToken(username, PROVIDER_ID)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val response = calendar.events().quickAdd(calendarId, text).apply {
                this.sendUpdates = sendUpdates
                this.sendNotifications = sendNotifications
            }.execute()
            response
        } catch (expected: Exception) {
            log.error("Error while getting events for calendarId=$calendarId", expected)
            null
        }
    }

    fun getAvailableColors(
        username: String,
    ): Colors? {
        return try {
            val accessToken = oAuth2Service.getAccessToken(username, PROVIDER_ID)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val response = calendar.colors().get().execute()
            response
        } catch (expected: Exception) {
            log.error("Error while getting available colors: ", expected)
            null
        }
    }

    fun deleteEventById(
        username: String,
        calendarId: String,
        eventId: String,
    ) {
        try {
            val accessToken = oAuth2Service.getAccessToken(username, PROVIDER_ID)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            calendar.events().delete(calendarId, eventId).execute()
        } catch (expected: Exception) {
            log.error("Error while deleting event with id={} from calendarId={}", eventId, calendarId, expected)
        }
    }

    fun createEvent(
        username: String,
        calendarId: String,
        request: CreateEventRequest,
    ): Event? {
        return try {
            val accessToken = oAuth2Service.getAccessToken(username, PROVIDER_ID)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val eventToCreate: Event = generateEvent(request)
            calendar.events().insert(calendarId, eventToCreate).execute()
        } catch (expected: Exception) {
            log.error("Error while creating event in calendarId={}", calendarId, expected)
            null
        }
    }

    fun patchEvent(
        username: String,
        calendarId: String,
        eventId: String,
        request: CreateEventRequest,
    ): Event? {
        return try {
            val accessToken = oAuth2Service.getAccessToken(username, PROVIDER_ID)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val existingEvent = calendar.events().get(calendarId, eventId).execute()
            request.start.let { existingEvent.start = it.toGoogleEventDateTime() }
            request.end.let { existingEvent.end = it.toGoogleEventDateTime() }
            request.summary.let { existingEvent.summary = it }
            request.description.let { existingEvent.description = it }
            request.eventType.let { existingEvent.eventType = it }
            request.recurrence.let { existingEvent.recurrence = it }

            calendar.events().patch(calendarId, eventId, existingEvent).execute()
        } catch (e: GoogleJsonResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND.value()) {
                log.warn("External event {} not found in calendar {}, was likely deleted", eventId, calendarId)
                throw ExternalEventNotFoundException(eventId)
            }
            log.error("Error while patching event {} in calendarId={}", eventId, calendarId, e)
            null
        } catch (expected: Exception) {
            log.error("Error while patching event {} in calendarId={}", eventId, calendarId, expected)
            null
        }
    }

    private fun generateEvent(request: CreateEventRequest): Event {
        val event = Event().apply {
            setRequiredFields(this, request)
            summary = request.summary
            eventType = request.eventType
            description = request.description
            recurrence = request.recurrence
            attendees = getAttendees(request.attendees)
        }

        return event
    }

    private fun setRequiredFields(event: Event, request: CreateEventRequest): Event {
        event.setStart(
            request.start.toGoogleEventDateTime()
        )

        event.setEnd(
            request.end.toGoogleEventDateTime()
        )

        return event
    }

    private fun getAttendees(attendees: List<Attendee>?): List<EventAttendee>? {
        if (attendees == null) {
            return null
        }
        return attendees.map { attendee ->
            EventAttendee()
                .setEmail(attendee.email)
                .setDisplayName(attendee.displayName)
                .setComment(attendee.comment)
                .setAdditionalGuests(attendee.additionalGuests)
                .setOptional(attendee.optional)
                .setResource(attendee.resource)
                .setResponseStatus(attendee.responseStatus)
        }
    }
}
