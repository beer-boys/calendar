package ru.itmo.dws.calendar.provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.service.GoogleCalendarFactory

@Component
class GoogleCalendarProvider(
    private val clientService: OAuth2AuthorizedClientService,
    private val googleCalendarFactory: GoogleCalendarFactory,
) : CalendarProvider {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun getCalendars(
        authentication: OAuth2AuthenticationToken
    ): Map<String, String> {
        val accessToken = getAccessTokenByAuthentication(authentication)
        val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

        val response = calendar.calendarList().list().execute()
        return response.items.associate {
            it.summary to it.id
        }
    }

    override fun getCalendarById(
        authentication: OAuth2AuthenticationToken,
        calendarId: String
    ): String {
        return try {
            val accessToken = getAccessTokenByAuthentication(authentication)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val response = calendar.calendarList().get(calendarId).execute()
            response.toPrettyString()
        } catch (expected: Exception) {
            log.error("Error while getting calendar $calendarId: ", expected)
            ""
        }
    }

    override fun getEventsByCalendarId(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
    ): String {
        return try {
            val accessToken = getAccessTokenByAuthentication(authentication)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val response = calendar.events().list(calendarId).execute()
            response.toPrettyString()
        } catch (expected: Exception) {
            log.error("Error while getting events for calendarId=$calendarId", expected)
            ""
        }
    }

    override fun getEventByEventIdAndCalendarId(
        authentication: OAuth2AuthenticationToken,
        calendarId: String,
        eventId: String
    ): String {
        return try {
            val accessToken = getAccessTokenByAuthentication(authentication)
            val calendar = googleCalendarFactory.createWithAccessToken(accessToken)

            val response = calendar.events().get(calendarId, eventId).execute()
            response.toPrettyString()
        } catch (expected: Exception) {
            log.error("Error while getting events for calendarId=$calendarId", expected)
            ""
        }
    }

    private fun getAccessTokenByAuthentication(
        authentication: OAuth2AuthenticationToken
    ): String {
        val client = clientService.loadAuthorizedClient<OAuth2AuthorizedClient>(
            authentication.authorizedClientRegistrationId,
            authentication.name
        )
        return client.accessToken?.tokenValue
            ?: error("No access token found")
    }
}
