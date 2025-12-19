package ru.itmo.dws.calendar.infrastructure.calendar

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken

object CalendarAuthContext {

    private val authHolder = ThreadLocal<OAuth2AuthenticationToken?>()

    fun set(token: OAuth2AuthenticationToken?) {
        authHolder.set(token)
    }

    fun get(): OAuth2AuthenticationToken? = authHolder.get()

    fun clear() {
        authHolder.remove()
    }

    fun requireAuth(): OAuth2AuthenticationToken {
        return get() ?: throw IllegalStateException(
            "OAuth2 authentication required but not available in context. " +
                "User must be authenticated via OAuth2 to access external calendar."
        )
    }
}
