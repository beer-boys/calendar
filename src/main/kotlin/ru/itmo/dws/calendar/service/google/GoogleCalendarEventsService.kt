package ru.itmo.dws.calendar.service.google

import com.google.api.services.calendar.model.Event
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service
import ru.itmo.dws.calendar.dto.google.CreateEventRequest
import ru.itmo.dws.calendar.provider.GoogleCalendarProvider

@Service
class GoogleCalendarEventsService(
    private val googleCalendarProvider: GoogleCalendarProvider,
) {
    fun getEventsByCalendarId(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
    ): String {
        return googleCalendarProvider.getEventsByCalendarId(authentication, calendarId)
    }

    fun getEventByEventIdAndCalendarId(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
        eventId: String
    ): String {
        return googleCalendarProvider.getEventByEventIdAndCalendarId(authentication, calendarId, eventId)
    }

    fun quickAddEvent(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
        text: String,
        sendNotifications: Boolean? = null,
        sendUpdates: String? = null
    ): Event? {
        return googleCalendarProvider.quickAdd(authentication, calendarId, text)
    }

    fun deleteEventById(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
        eventId: String,
    ) {
        return googleCalendarProvider.deleteEventById(authentication, calendarId, eventId)
    }

    fun createEvent(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
        request: CreateEventRequest,
        conferenceDataVersion: Int?,
        maxAttendees: Int?,
        sendNotifications: Boolean?,
        // can be 'all' | 'externalOnly' | 'none'
        sendUpdates: String?,
        supportsAttachments: Boolean?,
    ): Event? {
        return googleCalendarProvider.createEvent(
            authentication,
            calendarId,
            request
        )
    }

    fun patchEvent(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
        eventId: String,
        request: CreateEventRequest,
        conferenceDataVersion: Int?,
        maxAttendees: Int?,
        sendNotifications: Boolean?,
        // can be 'all' | 'externalOnly' | 'none'
        sendUpdates: String?,
        supportsAttachments: Boolean?,
    ): Event? {
        return googleCalendarProvider.patchEvent(
            authentication,
            calendarId,
            eventId,
            request
        )
    }
}
