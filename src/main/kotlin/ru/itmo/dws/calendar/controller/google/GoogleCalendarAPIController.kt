package ru.itmo.dws.calendar.controller.google

import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.model.CalendarListEntry
import com.google.api.services.calendar.model.Colors
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.Events
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.itmo.dws.calendar.configuration.BasePath
import ru.itmo.dws.calendar.dto.google.CreateEventRequest
import ru.itmo.dws.calendar.service.google.GoogleCalendarColorsService
import ru.itmo.dws.calendar.service.google.GoogleCalendarEventsService
import ru.itmo.dws.calendar.service.google.GoogleCalendarService

@RestController
@RequestMapping(BasePath.GOOGLE_BASE)
class GoogleCalendarAPIController(
    private val googleCalendarService: GoogleCalendarService,
    private val googleCalendarEventsService: GoogleCalendarEventsService,
    private val googleCalendarColorsService: GoogleCalendarColorsService,
    private val gsonFactory: GsonFactory,
) {

    // just for debug
    @GetMapping("/token")
    fun getToken(
        @RegisteredOAuth2AuthorizedClient("google") client: OAuth2AuthorizedClient
    ): Pair<String, String> {
        return Pair<String, String>(client.accessToken.tokenValue, client.refreshToken!!.tokenValue)
    }

    // calendar region
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
    ): ResponseEntity<CalendarListEntry> {
        return ResponseEntity.ok(
            gsonFactory.fromString(
                googleCalendarService.getCalendarById(authentication, calendarId),
                CalendarListEntry::class.java
            )
        )
    }
    // end region

    // calendar events region
    @GetMapping("/calendars/{calendarId}/events")
    fun getCalendarEvents(
        authentication: OAuth2AuthenticationToken,
        @PathVariable calendarId: String
    ): ResponseEntity<Events> {
        return ResponseEntity.ok(
            gsonFactory.fromString(
                googleCalendarEventsService.getEventsByCalendarId(authentication, calendarId),
                Events::class.java
            )
        )
    }

    @GetMapping("/calendars/{calendarId}/events/{eventId}")
    fun getCalendarEventById(
        authentication: OAuth2AuthenticationToken,
        @PathVariable calendarId: String,
        @PathVariable eventId: String,
    ): ResponseEntity<Event> {
        return ResponseEntity.ok(
            gsonFactory.fromString(
                googleCalendarEventsService.getEventByEventIdAndCalendarId(authentication, calendarId, eventId),
                Event::class.java
            )
        )
    }

    @PostMapping("/calendars/{calendarId}/events/quickAdd")
    fun quickAdd(
        authentication: OAuth2AuthenticationToken,
        @PathVariable calendarId: String,
        @RequestParam text: String,
        @RequestParam(required = false) sendNotifications: Boolean?,
        // can be 'all' | 'externalOnly' | 'none'
        @RequestParam(required = false) sendUpdates: String?
    ): ResponseEntity<Event?> {
        return ResponseEntity.ok(
            googleCalendarEventsService.quickAddEvent(authentication, calendarId, text, sendNotifications, sendUpdates)
        )
    }

    @DeleteMapping("/calendars/{calendarId}/events/{eventId}")
    fun deleteEventById(
        authentication: OAuth2AuthenticationToken,
        @PathVariable calendarId: String,
        @PathVariable eventId: String,
    ): ResponseEntity<String> {
        googleCalendarEventsService.deleteEventById(authentication, calendarId, eventId)
        return ResponseEntity.ok("ok")
    }

    @PostMapping("/calendars/{calendarId}/events")
    fun createEvent(
        authentication: OAuth2AuthenticationToken,
        @PathVariable calendarId: String,
        @RequestBody request: CreateEventRequest,
        @RequestParam(required = false) conferenceDataVersion: Int?,
        @RequestParam(required = false) maxAttendees: Int?,
        @RequestParam(required = false) sendNotifications: Boolean?,
        // can be 'all' | 'externalOnly' | 'none'
        @RequestParam(required = false) sendUpdates: String?,
        @RequestParam(required = false) supportsAttachments: Boolean?,
    ): ResponseEntity<Event?> {
        return ResponseEntity.ok(
            googleCalendarEventsService.createEvent(
                authentication,
                calendarId,
                request,
                conferenceDataVersion,
                maxAttendees,
                sendNotifications,
                sendUpdates,
                supportsAttachments
            )
        )
    }

    @PatchMapping("/calendars/{calendarId}/events/{eventId}")
    fun patchEvent(
        authentication: OAuth2AuthenticationToken,
        @PathVariable calendarId: String,
        @PathVariable eventId: String,
        @RequestBody request: CreateEventRequest,
        @RequestParam(required = false) conferenceDataVersion: Int?,
        @RequestParam(required = false) maxAttendees: Int?,
        @RequestParam(required = false) sendNotifications: Boolean?,
        // can be 'all' | 'externalOnly' | 'none'
        @RequestParam(required = false) sendUpdates: String?,
        @RequestParam(required = false) supportsAttachments: Boolean?,
    ): ResponseEntity<Event?> {
        return ResponseEntity.ok(
            googleCalendarEventsService.patchEvent(
                authentication,
                calendarId,
                eventId,
                request,
                conferenceDataVersion,
                maxAttendees,
                sendNotifications,
                sendUpdates,
                supportsAttachments
            )
        )
    }
    // end region

    // colors region
    @GetMapping("/colors")
    fun getAvailableColors(
        authentication: OAuth2AuthenticationToken,
    ): ResponseEntity<Colors> {
        return ResponseEntity.ok(
            googleCalendarColorsService.getAvailableColors(authentication)
        )
    }
    // end region
}
