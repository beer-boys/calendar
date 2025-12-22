package ru.itmo.dws.calendar.core.domain.valueobject.room

import java.util.UUID

@JvmInline
value class MeetingRoomId(val value: UUID) {

    companion object {
        fun generate(): MeetingRoomId = MeetingRoomId(UUID.randomUUID())
        fun of(value: String): MeetingRoomId = MeetingRoomId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}
