package ru.itmo.dws.calendar.service.google

import com.google.api.services.calendar.model.Colors
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service
import ru.itmo.dws.calendar.provider.GoogleCalendarProvider

@Service
class GoogleCalendarColorsService(
    private val googleCalendarProvider: GoogleCalendarProvider,
) {
    fun getAvailableColors(
        authentication: OAuth2AuthenticationToken,
    ): Colors? {
        return googleCalendarProvider.getAvailableColors(authentication)
    }
}
