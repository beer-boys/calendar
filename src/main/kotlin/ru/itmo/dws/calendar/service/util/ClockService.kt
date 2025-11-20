package ru.itmo.dws.calendar.service.util

import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

open class ClockService(
    private val clock: Clock
) {

    open fun now(): LocalDateTime {
        return clock.instant()
            .atOffset(offset())
            .toLocalDateTime()
    }

    open fun offset(): ZoneOffset {
        return ZoneOffset.UTC
    }
}
