package ru.itmo.dws.calendar.core.domain.exception

import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId

class MeetingRoomNotFound(roomId: MeetingRoomId) :
    CalendarDomainException("Meeting room not found: $roomId")

class MeetingRoomInactive(roomId: MeetingRoomId) :
    CalendarDomainException("Meeting room is not active: $roomId")

class TimeSlotNotAvailable(roomId: MeetingRoomId, slot: TimeSlot) :
    CalendarDomainException("Time slot not available for room $roomId: $slot")

class BookingNotFound(bookingId: Any) :
    CalendarDomainException("Booking not found: $bookingId")
