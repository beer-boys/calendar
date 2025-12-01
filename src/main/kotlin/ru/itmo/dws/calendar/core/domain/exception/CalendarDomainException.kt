package ru.itmo.dws.calendar.core.domain.exception

abstract class CalendarDomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
