package ru.itmo.dws.calendar.controller

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itmo.dws.calendar.configuration.BasePath
import ru.itmo.dws.calendar.service.GoogleCalendarEventsService
import ru.itmo.dws.calendar.service.GoogleCalendarService

@RestController
@RequestMapping(BasePath.GOOGLE_BASE)
class GoogleCalendarAPIController(
    private val googleCalendarService: GoogleCalendarService,
    private val googleCalendarEventsService: GoogleCalendarEventsService,
) {

    @GetMapping("/calendars")
    fun getCalendars(
        authentication: OAuth2AuthenticationToken,
    ): Map<String, String> {
        return googleCalendarService.getCalendars(authentication)
    }

    @GetMapping("/calendars/{calendarId}")
    fun getCalendarById(
        authentication: OAuth2AuthenticationToken,
        @PathVariable calendarId: String
    ): String {
        return googleCalendarService.getCalendarById(authentication, calendarId)
    }

    @GetMapping("/calendars/{calendarId}/events")
    fun getCalendarEvents(
        authentication: OAuth2AuthenticationToken,
        @PathVariable calendarId: String
    ): String {
        return googleCalendarEventsService.getEventsByCalendarId(authentication, calendarId)
    }

    @GetMapping("/calendars/{calendarId}/events/{eventId}")
    fun getCalendarEventById(
        authentication: OAuth2AuthenticationToken,
        @PathVariable calendarId: String,
        @PathVariable eventId: String,
    ): String {
        return googleCalendarEventsService.getEventByEventIdAndCalendarId(authentication, calendarId, eventId)
    }
}
