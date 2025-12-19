package ru.itmo.dws.calendar.service.google

import com.google.api.services.calendar.model.Colors
import org.springframework.stereotype.Service
import ru.itmo.dws.calendar.provider.GoogleCalendarProvider

@Service
class GoogleCalendarColorsService(
    private val googleCalendarProvider: GoogleCalendarProvider,
) {
    fun getAvailableColors(
        username: String,
    ): Colors? {
        return googleCalendarProvider.getAvailableColors(username)
    }
}
