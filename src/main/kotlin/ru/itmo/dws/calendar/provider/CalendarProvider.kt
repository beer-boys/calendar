package ru.itmo.dws.calendar.provider

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken

@Suppress("ForbiddenComment")
interface CalendarProvider {
    // todo replace with own model
    fun getCalendars(
        authentication: OAuth2AuthenticationToken,
    ): Map<String, String>

    fun getCalendarById(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
    ): String

    fun getEventsByCalendarId(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
    ): String

    fun getEventByEventIdAndCalendarId(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
        eventId: String
    ): String
}
