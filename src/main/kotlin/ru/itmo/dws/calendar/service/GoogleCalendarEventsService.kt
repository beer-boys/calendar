package ru.itmo.dws.calendar.service

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service
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
}
