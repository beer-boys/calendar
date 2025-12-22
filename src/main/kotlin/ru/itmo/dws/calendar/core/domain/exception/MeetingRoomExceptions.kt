package ru.itmo.dws.calendar.core.domain.exception

import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId

class MeetingRoomNotFound(roomId: MeetingRoomId) :
    RuntimeException("Meeting room not found: $roomId")

class MeetingRoomInactive(roomId: MeetingRoomId) :
    RuntimeException("Meeting room is not active: $roomId")

class TimeSlotNotAvailable(roomId: MeetingRoomId, slot: TimeSlot) :
    RuntimeException("Time slot not available for room $roomId: $slot")

class BookingNotFound(bookingId: Any) :
    RuntimeException("Booking not found: $bookingId")
