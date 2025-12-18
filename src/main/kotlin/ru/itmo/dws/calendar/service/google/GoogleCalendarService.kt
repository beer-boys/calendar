package ru.itmo.dws.calendar.service.google

import org.springframework.stereotype.Service
import ru.itmo.dws.calendar.provider.GoogleCalendarProvider

@Service
class GoogleCalendarService(
    private val googleCalendarProvider: GoogleCalendarProvider,
) {
    fun getCalendars(
        username: String,
    ): Map<String, String> {
        return googleCalendarProvider.getCalendars(username)
    }

    fun getCalendarById(
        username: String,
        calendarId: String,
    ): String {
        return googleCalendarProvider.getCalendarById(username, calendarId)
    }
}
