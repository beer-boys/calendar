package ru.itmo.dws.calendar.service.google

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import org.springframework.stereotype.Component

@Component
class GoogleCalendarFactory(
    private val gsonFactory: GsonFactory,
) {
    private val transport: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()

    companion object {
        private const val APP_NAME = "itmo-calendar"
    }

    fun createWithAccessToken(accessToken: String): Calendar {
        return Calendar.Builder(
            transport,
            gsonFactory,
            HttpCredentialsAdapter(
                GoogleCredentials.create(AccessToken(accessToken, null))
            )
        ).setApplicationName(APP_NAME)
            .build()
    }
}
