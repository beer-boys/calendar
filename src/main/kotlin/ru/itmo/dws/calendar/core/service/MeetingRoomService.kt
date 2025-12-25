package ru.itmo.dws.calendar.core.service

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.springframework.transaction.annotation.Transactional
import ru.itmo.dws.calendar.core.domain.exception.BookingNotFound
import ru.itmo.dws.calendar.core.domain.exception.MeetingRoomInactive
import ru.itmo.dws.calendar.core.domain.exception.MeetingRoomNotFound
import ru.itmo.dws.calendar.core.domain.model.MeetingRoom
import ru.itmo.dws.calendar.core.domain.model.MeetingRoom.MeetingRoomStatus
import ru.itmo.dws.calendar.core.domain.model.MeetingRoomBooking
import ru.itmo.dws.calendar.core.domain.model.MeetingRoomBooking.BookingStatus
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomBookingId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomSearchCriteria
import ru.itmo.dws.calendar.core.port.input.room.BookMeetingRoomCommand
import ru.itmo.dws.calendar.core.port.input.room.CancelMeetingRoomBookingCommand
import ru.itmo.dws.calendar.core.port.input.room.MeetingRoomBookingUseCase
import ru.itmo.dws.calendar.core.port.input.room.MeetingRoomQueryUseCase
import ru.itmo.dws.calendar.core.port.output.room.MeetingRoomBookingProvider
import ru.itmo.dws.calendar.core.port.output.room.MeetingRoomProvider
import ru.itmo.dws.calendar.core.service.utils.TimeSlotUtils.clipToWindow
import ru.itmo.dws.calendar.core.service.utils.TimeSlotUtils.mergeOverlappingOrAdjacent
import ru.itmo.dws.calendar.core.service.utils.TimeSlotUtils.sliceWindow
import ru.itmo.dws.calendar.core.service.utils.TimeSlotUtils.subtractBusyFromWindow

open class MeetingRoomService(
    private val roomProvider: MeetingRoomProvider,
    private val bookingProvider: MeetingRoomBookingProvider,
    private val slotStep: Duration = Duration.ofMinutes(15),
    private val defaultZone: ZoneId = ZoneId.of("UTC"),
) : MeetingRoomQueryUseCase, MeetingRoomBookingUseCase {

    override fun findRooms(criteria: MeetingRoomSearchCriteria): List<MeetingRoom> {
        return roomProvider.findAllByCriteria(criteria)
    }

    override fun findAvailableSlots(roomId: MeetingRoomId, date: LocalDate, duration: Duration): List<TimeSlot> {
        require(duration.isPositive) { "duration must be positive" }
        require(slotStep.isPositive) { "slotStep must be positive" }

        val room = roomProvider.findById(roomId) ?: throw MeetingRoomNotFound(roomId)
        if (room.status != MeetingRoomStatus.ACTIVE) return emptyList()

        val zone = room.location.timeZoneId ?: defaultZone

        val dayStart = ZonedDateTime.of(date, LocalTime.MIDNIGHT, zone)
        val dayEnd = dayStart.plusDays(1)

        val busySlots = bookingProvider.findBookingsInRange(roomId, dayStart, dayEnd)
            .asSequence()
            .filter { it.status == BookingStatus.CONFIRMED }
            .map { it.timeSlot }
            .map { clipToWindow(it, dayStart, dayEnd) }
            .filterNotNull()
            .sortedBy { it.start }
            .toList()

        val mergedBusy = mergeOverlappingOrAdjacent(busySlots)
        val freeWindows = subtractBusyFromWindow(TimeSlot(dayStart, dayEnd), mergedBusy)

        return freeWindows.flatMap { window ->
            sliceWindow(
                window = window,
                duration = duration,
                step = slotStep,
                alignmentBase = dayStart,
            )
        }
    }

    override fun findAvailableRooms(timeSlot: TimeSlot, criteria: MeetingRoomSearchCriteria?): List<MeetingRoom> {
        val rooms = (
            criteria?.let { roomProvider.findAllByCriteria(it) }
                ?: roomProvider.findAllByCriteria(MeetingRoomSearchCriteria(status = MeetingRoomStatus.ACTIVE))
            )
            .filter { it.status == MeetingRoomStatus.ACTIVE }

        val busy = bookingProvider.findBusyRoomIds(rooms.map { it.id }.toSet(), timeSlot)
        return rooms.filterNot { busy.contains(it.id) }
    }

    @Transactional
    override fun bookRoom(command: BookMeetingRoomCommand): MeetingRoomBooking {
        val room = roomProvider.findById(command.roomId) ?: throw MeetingRoomNotFound(command.roomId)
        if (room.status != MeetingRoomStatus.ACTIVE) throw MeetingRoomInactive(command.roomId)

        val booking = MeetingRoomBooking(
            id = MeetingRoomBookingId.generate(),
            roomId = command.roomId,
            organizerId = command.organizerId,
            purpose = command.purpose,
            status = BookingStatus.CONFIRMED,
            timeSlot = command.timeSlot
        )

        return bookingProvider.create(booking)
    }

    override fun cancelBooking(command: CancelMeetingRoomBookingCommand) {
        bookingProvider.cancel(command.bookingId)
    }

    override fun getBooking(bookingId: MeetingRoomBookingId): MeetingRoomBooking {
        return bookingProvider.findById(bookingId) ?: throw BookingNotFound(bookingId)
    }
}
