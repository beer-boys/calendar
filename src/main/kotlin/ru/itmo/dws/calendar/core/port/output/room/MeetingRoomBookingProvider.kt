package ru.itmo.dws.calendar.core.port.output.room

import java.time.ZonedDateTime
import ru.itmo.dws.calendar.core.domain.model.MeetingRoomBooking
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomBookingId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId

interface MeetingRoomBookingProvider {

    fun findById(bookingId: MeetingRoomBookingId): MeetingRoomBooking?

    fun findBookingsInRange(
        roomId: MeetingRoomId,
        fromInclusive: ZonedDateTime,
        toExclusive: ZonedDateTime,
    ): List<MeetingRoomBooking>

    fun findBusyRoomIds(
        roomIds: Set<MeetingRoomId>,
        timeSlot: TimeSlot,
    ): Set<MeetingRoomId>

    fun create(booking: MeetingRoomBooking): MeetingRoomBooking

    fun cancel(bookingId: MeetingRoomBookingId)
}
