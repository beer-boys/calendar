package ru.itmo.dws.calendar.core.domain.exception

class ExternalEventNotFoundException(
    val externalEventId: String,
    message: String = "External event not found: $externalEventId"
) : CalendarDomainException(message)

