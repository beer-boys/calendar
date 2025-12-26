package ru.itmo.dws.calendar.dto.room.booking

import ru.itmo.dws.calendar.core.domain.model.MeetingRoomBooking

enum class BookingStatusDto {
    CONFIRMED,
    CANCELED,
    ;

    companion object {
        fun from(value: MeetingRoomBooking.BookingStatus) = when (value) {
            MeetingRoomBooking.BookingStatus.CONFIRMED -> CONFIRMED
            MeetingRoomBooking.BookingStatus.CANCELED -> CANCELED
        }

        fun fromString(value: String): BookingStatusDto = BookingStatusDto.valueOf(value.uppercase())
    }
}
