package ru.itmo.dws.calendar.core.domain.exception

class ExternalCalendarSyncException(
    message: String,
    cause: Throwable? = null
) : CalendarDomainException(message, cause)
