package ru.itmo.dws.calendar.core.domain.valueobject.room

import java.util.UUID

@JvmInline
value class MeetingRoomBookingId(val value: UUID) {

    companion object {
        fun generate(): MeetingRoomBookingId = MeetingRoomBookingId(UUID.randomUUID())
        fun of(value: String): MeetingRoomBookingId = MeetingRoomBookingId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}
