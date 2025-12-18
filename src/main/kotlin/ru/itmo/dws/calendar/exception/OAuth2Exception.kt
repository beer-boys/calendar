package ru.itmo.dws.calendar.exception

import org.springframework.http.HttpStatus

class OAuth2Exception(
    username: String,
    provider: String,
) : CalendarException("User with username = $username and provider = $provider wasn't found") {
    override val statusCode: HttpStatus = HttpStatus.UNAUTHORIZED
}
