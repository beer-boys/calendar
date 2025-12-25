package ru.itmo.dws.calendar.core.service.provider

import java.time.ZoneId
import java.time.ZonedDateTime
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import ru.itmo.dws.calendar.core.domain.exception.TimeSlotNotAvailable
import ru.itmo.dws.calendar.core.domain.model.MeetingRoomBooking
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomBookingId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId
import ru.itmo.dws.calendar.core.port.output.room.MeetingRoomBookingProvider
import ru.itmo.dws.calendar.core.port.output.room.MeetingRoomProvider
import ru.itmo.dws.calendar.mapper.toDomain
import ru.itmo.dws.calendar.mapper.toEntity
import ru.itmo.dws.calendar.repository.MeetingRoomBookingRepository

@Component
class DatabaseMeetingRoomBookingProvider(
    private val repository: MeetingRoomBookingRepository,
    private val meetingRoomProvider: MeetingRoomProvider,
) : MeetingRoomBookingProvider {

    companion object {
        private val logger = LoggerFactory.getLogger(DatabaseMeetingRoomBookingProvider::class.java)
        private val UTC_TIME_ZONE = ZoneId.of("UTC")
    }

    override fun findById(bookingId: MeetingRoomBookingId): MeetingRoomBooking? {
        val entity = repository.findById(bookingId.value).orElse(null) ?: return null
        val room = meetingRoomProvider.findById(MeetingRoomId(entity.roomId)) ?: return null
        val zone = room.location.timeZoneId ?: UTC_TIME_ZONE
        return entity.toDomain(zone)
    }

    override fun findBookingsInRange(
        roomId: MeetingRoomId,
        fromInclusive: ZonedDateTime,
        toExclusive: ZonedDateTime,
    ): List<MeetingRoomBooking> {
        val room = meetingRoomProvider.findById(roomId) ?: return emptyList()
        val zone = room.location.timeZoneId ?: UTC_TIME_ZONE

        val list = repository.findConfirmedOverlappingInRange(
            roomId = roomId.value,
            fromInclusive = fromInclusive.toInstant(),
            toExclusive = toExclusive.toInstant(),
        )
        return list.map { it.toDomain(zone) }
    }

    override fun findBusyRoomIds(roomIds: Set<MeetingRoomId>, timeSlot: TimeSlot): Set<MeetingRoomId> {
        if (roomIds.isEmpty()) return emptySet()
        val busy = repository.findBusyRoomIds(
            roomIds = roomIds.map { it.value },
            startTime = timeSlot.start.toInstant(),
            endTime = timeSlot.end.toInstant(),
        )
        return busy.map { MeetingRoomId(it) }.toSet()
    }

    override fun create(booking: MeetingRoomBooking): MeetingRoomBooking {
        try {
            repository.insert(booking.toEntity())
            return booking
        } catch (e: DbActionExecutionException) {
            logger.info("Failed to create booking: {}", booking, e)
            if (isOverlapConstraintViolation(e)) {
                throw TimeSlotNotAvailable(booking.roomId, booking.timeSlot)
            }
            throw e
        } catch (e: DataIntegrityViolationException) {
            logger.info("Failed to create booking: {}", booking, e)
            if (isOverlapConstraintViolation(e)) {
                throw TimeSlotNotAvailable(booking.roomId, booking.timeSlot)
            }
            throw e
        }
    }

    @Transactional
    override fun cancel(bookingId: MeetingRoomBookingId) {
        val existing = repository.findById(bookingId.value).orElse(null) ?: return
        repository.save(existing.copy(status = "CANCELED"))
    }

    private fun isOverlapConstraintViolation(e: Throwable): Boolean {
        val root = rootCause(e)

        if (root is PSQLException && root.sqlState == "23P01") return true

        val msg = (root.message ?: "") + " " + (e.message ?: "")
        return msg.contains("ex_bookings_no_overlap", ignoreCase = true)
    }

    private tailrec fun rootCause(t: Throwable): Throwable {
        val cause = t.cause
        return if (cause == null || cause === t) t else rootCause(cause)
    }
}
