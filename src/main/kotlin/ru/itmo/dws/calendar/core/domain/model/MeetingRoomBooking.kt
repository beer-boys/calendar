package ru.itmo.dws.calendar.core.domain.model

import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomBookingId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId

data class MeetingRoomBooking(
    val id: MeetingRoomBookingId,
    val roomId: MeetingRoomId,
    val timeSlot: TimeSlot,
    val organizerId: UserId,
    val purpose: String? = null,
    val status: BookingStatus = BookingStatus.CONFIRMED
) {

    enum class BookingStatus {
        CONFIRMED,
        CANCELED
    }

    init {
        require(purpose == null || purpose.isNotBlank()) { "Purpose cannot be blank if provided" }
    }
}
