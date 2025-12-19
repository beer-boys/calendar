package ru.itmo.dws.calendar.exception

import org.springframework.http.HttpStatus

abstract class NotFoundException(
    msg: String,
    cause: Throwable? = null
) : CalendarException(msg, cause) {
    override val statusCode: HttpStatus = HttpStatus.NOT_FOUND
}
