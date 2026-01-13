package ru.itmo.dws.calendar.dto

import java.time.ZonedDateTime
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot

data class TimeSlotDto(
    val start: ZonedDateTime,
    val end: ZonedDateTime,
) {

    companion object {
        fun from(slot: TimeSlot): TimeSlotDto {
            return TimeSlotDto(slot.start, slot.end)
        }
    }
}
