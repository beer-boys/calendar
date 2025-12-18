package ru.itmo.dws.calendar.exception

import org.springframework.http.HttpStatus

open class CalendarException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    open val statusCode: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
}
