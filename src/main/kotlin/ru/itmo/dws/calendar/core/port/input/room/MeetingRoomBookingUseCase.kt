package ru.itmo.dws.calendar.core.port.input.room

import ru.itmo.dws.calendar.core.domain.model.MeetingRoomBooking
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomBookingId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId

data class BookMeetingRoomCommand(
    val roomId: MeetingRoomId,
    val timeSlot: TimeSlot,
    val organizerId: UserId,
    val purpose: String? = null,
    val sourceMeetingId: String? = null,
)

data class CancelMeetingRoomBookingCommand(
    val bookingId: MeetingRoomBookingId,
    val cancelledBy: UserId? = null,
    val reason: String? = null,
)

interface MeetingRoomBookingUseCase {
    fun bookRoom(command: BookMeetingRoomCommand): MeetingRoomBooking

    fun cancelBooking(command: CancelMeetingRoomBookingCommand)

    fun getBooking(bookingId: MeetingRoomBookingId, userId: UserId): MeetingRoomBooking
}
