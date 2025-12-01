package ru.itmo.dws.calendar.service.google

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service
import ru.itmo.dws.calendar.provider.GoogleCalendarProvider

@Service
class GoogleCalendarService(
    private val googleCalendarProvider: GoogleCalendarProvider,
) {
    fun getCalendars(
        authentication: OAuth2AuthenticationToken
    ): Map<String, String> {
        return googleCalendarProvider.getCalendars(authentication)
    }

    fun getCalendarById(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
    ): String {
        return googleCalendarProvider.getCalendarById(authentication, calendarId)
    }
}
