package ru.itmo.dws.calendar.domain.valueobject

import java.util.UUID

@JvmInline
value class RoomId(val value: UUID) {
    companion object {
        fun generate(): RoomId = RoomId(UUID.randomUUID())
        fun of(value: String): RoomId = RoomId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}
