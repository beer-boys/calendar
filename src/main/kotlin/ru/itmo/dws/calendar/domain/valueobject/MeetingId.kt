package ru.itmo.dws.calendar.domain.valueobject

import java.util.UUID

@JvmInline
value class MeetingId(val value: UUID) {
    companion object {
        fun generate(): MeetingId = MeetingId(UUID.randomUUID())
        fun of(value: String): MeetingId = MeetingId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}
