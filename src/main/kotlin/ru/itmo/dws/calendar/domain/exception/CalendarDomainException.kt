package ru.itmo.dws.calendar.domain.exception

abstract class CalendarDomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
