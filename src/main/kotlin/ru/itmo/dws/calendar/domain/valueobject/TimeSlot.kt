package ru.itmo.dws.calendar.domain.valueobject

import java.time.Duration
import java.time.ZonedDateTime

data class TimeSlot(
    val start: ZonedDateTime,
    val end: ZonedDateTime
) {
    init {
        require(end.isAfter(start)) {
            "End time must be after start time. Start: $start, End: $end"
        }
    }

    fun duration(): Duration = Duration.between(start, end)

    fun overlapsWith(other: TimeSlot): Boolean {
        return start.isBefore(other.end) && end.isAfter(other.start)
    }

    fun contains(other: TimeSlot): Boolean {
        return !start.isAfter(other.start) && !end.isBefore(other.end)
    }

    fun contains(time: ZonedDateTime): Boolean {
        return !time.isBefore(start) && time.isBefore(end)
    }

    fun shift(duration: Duration): TimeSlot {
        return TimeSlot(
            start = start.plus(duration),
            end = end.plus(duration)
        )
    }

    fun withBuffer(bufferDuration: BufferDuration): TimeSlot {
        return TimeSlot(
            start = start.minus(bufferDuration.before),
            end = end.plus(bufferDuration.after)
        )
    }

    companion object {
        fun of(start: ZonedDateTime, duration: Duration): TimeSlot {
            return TimeSlot(start, start.plus(duration))
        }
    }
}
