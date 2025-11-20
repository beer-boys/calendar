package ru.itmo.dws.calendar.exception

abstract class NotFoundException(
    msg: String,
    cause: Throwable? = null
) : RuntimeException(msg, cause)
