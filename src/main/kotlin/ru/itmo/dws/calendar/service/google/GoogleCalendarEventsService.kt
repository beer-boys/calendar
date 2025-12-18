package ru.itmo.dws.calendar.service.google

import com.google.api.services.calendar.model.Event
import org.springframework.stereotype.Service
import ru.itmo.dws.calendar.dto.google.CreateEventRequest
import ru.itmo.dws.calendar.provider.GoogleCalendarProvider

@Service
class GoogleCalendarEventsService(
    private val googleCalendarProvider: GoogleCalendarProvider,
) {
    fun getEventsByCalendarId(
        username: String,
        calendarId: String,
    ): String {
        return googleCalendarProvider.getEventsByCalendarId(username, calendarId)
    }

    fun getEventByEventIdAndCalendarId(
        username: String,
        calendarId: String,
        eventId: String
    ): String {
        return googleCalendarProvider.getEventByEventIdAndCalendarId(username, calendarId, eventId)
    }

    fun quickAddEvent(
        username: String,
        calendarId: String,
        text: String,
        sendNotifications: Boolean? = null,
        sendUpdates: String? = null
    ): Event? {
        return googleCalendarProvider.quickAdd(username, calendarId, text)
    }

    fun deleteEventById(
        username: String,
        calendarId: String,
        eventId: String,
    ) {
        return googleCalendarProvider.deleteEventById(username, calendarId, eventId)
    }

    fun createEvent(
        username: String,
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
            username,
            calendarId,
            request
        )
    }

    fun patchEvent(
        username: String,
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
            username,
            calendarId,
            eventId,
            request
        )
    }
}
