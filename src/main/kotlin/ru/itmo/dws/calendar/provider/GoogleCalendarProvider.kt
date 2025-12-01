package ru.itmo.dws.calendar.provider

import com.google.api.services.calendar.model.Colors
import com.google.api.services.calendar.model.Event
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.dto.google.CreateEventRequest
import ru.itmo.dws.calendar.dto.google.toGoogleEventDateTime
import ru.itmo.dws.calendar.service.google.GoogleCalendarFactory

@Component
@Suppress("TooManyFunctions")
class GoogleCalendarProvider(
    private val clientService: OAuth2AuthorizedClientService,
    private val googleCalendarFactory: GoogleCalendarFactory,
) : CalendarProvider {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun getCalendars(
        authentication: OAuth2AuthenticationToken
    ): Map<String, String> {
        val accessToken = getAccessTokenByAuthentication(authentication)
        val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

        val response = calendar.calendarList().list().execute()
        return response.items.associate {
            it.summary to it.id
        }
    }

    override fun getCalendarById(
        authentication: OAuth2AuthenticationToken,
        calendarId: String
    ): String {
        return try {
            val accessToken = getAccessTokenByAuthentication(authentication)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val response = calendar.calendarList().get(calendarId).execute()
            response.toPrettyString()
        } catch (expected: Exception) {
            log.error("Error while getting calendar $calendarId: ", expected)
            ""
        }
    }

    override fun getEventsByCalendarId(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
    ): String {
        return try {
            val accessToken = getAccessTokenByAuthentication(authentication)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val response = calendar.events().list(calendarId).execute()
            response.toPrettyString()
        } catch (expected: Exception) {
            log.error("Error while getting events for calendarId=$calendarId", expected)
            ""
        }
    }

    override fun getEventByEventIdAndCalendarId(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
        eventId: String
    ): String {
        return try {
            val accessToken = getAccessTokenByAuthentication(authentication)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val response = calendar.events().get(calendarId, eventId).execute()
            response.toPrettyString()
        } catch (expected: Exception) {
            log.error("Error while getting events for calendarId=$calendarId", expected)
            ""
        }
    }

    fun quickAdd(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
        text: String,
        sendNotifications: Boolean = false,
        sendUpdates: String = "none"
    ): Event? {
        return try {
            val accessToken = getAccessTokenByAuthentication(authentication)
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
        authentication: OAuth2AuthenticationToken,
    ): Colors? {
        return try {
            val accessToken = getAccessTokenByAuthentication(authentication)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val response = calendar.colors().get().execute()
            response
        } catch (expected: Exception) {
            log.error("Error while getting available colors: ", expected)
            null
        }
    }

    fun deleteEventById(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
        eventId: String,
    ) {
        try {
            val accessToken = getAccessTokenByAuthentication(authentication)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            calendar.events().delete(calendarId, eventId).execute()
        } catch (expected: Exception) {
            log.error("Error while deleting event with id={} from calendarId={}", eventId, calendarId, expected)
        }
    }

    fun createEvent(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
        request: CreateEventRequest,
    ): Event? {
        return try {
            val accessToken = getAccessTokenByAuthentication(authentication)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val eventToCreate: Event = generateEvent(request)
            calendar.events().insert(calendarId, eventToCreate).execute()
        } catch (expected: Exception) {
            log.error("Error while creating event in calendarId={}", calendarId, expected)
            null
        }
    }

    fun patchEvent(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
        eventId: String,
        request: CreateEventRequest,
    ): Event? {
        return try {
            val accessToken = getAccessTokenByAuthentication(authentication)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val existingEvent = calendar.events().get(calendarId, eventId).execute()
            request.start.let { existingEvent.start = it.toGoogleEventDateTime() }
            request.end.let { existingEvent.end = it.toGoogleEventDateTime() }
            request.summary.let { existingEvent.summary = it }
            request.description.let { existingEvent.description = it }
            request.eventType.let { existingEvent.eventType = it }
            request.recurrence.let { existingEvent.recurrence = it }

            calendar.events().patch(calendarId, eventId, existingEvent).execute()
        } catch (expected: Exception) {
            log.error("Error while creating event in calendarId={}", calendarId, expected)
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

    private fun getAccessTokenByAuthentication(
        authentication: OAuth2AuthenticationToken
    ): String {
        val client = clientService.loadAuthorizedClient<OAuth2AuthorizedClient>(
            authentication.authorizedClientRegistrationId,
            authentication.name
        )
        return client.accessToken?.tokenValue
            ?: error("No access token found")
    }
}
