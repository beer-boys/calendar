package ru.itmo.dws.calendar.core.domain.exception

class HabitValidationException(
    message: String,
    val field: String? = null
) : CalendarDomainException(message)
