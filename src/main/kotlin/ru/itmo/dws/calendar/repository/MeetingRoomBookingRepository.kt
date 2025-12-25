package ru.itmo.dws.calendar.repository

import java.time.Instant
import java.util.UUID
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.itmo.dws.calendar.model.MeetingRoomBookingEntity
import ru.itmo.dws.calendar.repository.extension.MeetingRoomBookingRepositoryExtension

interface MeetingRoomBookingRepository :
    MeetingRoomBookingRepositoryExtension, CrudRepository<MeetingRoomBookingEntity, UUID> {

    @Query(
        """
        select * from meeting_room_bookings
        where room_id = :roomId 
            and start_time < :toExclusive
            and end_time > :fromInclusive
            and status = 'CONFIRMED'
    """
    )
    fun findConfirmedOverlappingInRange(
        roomId: UUID,
        fromInclusive: Instant,
        toExclusive: Instant,
    ): List<MeetingRoomBookingEntity>

    @Query(
        """
        select distinct room_id from meeting_room_bookings
        where room_id in (:roomIds)
            and start_time < :endTime
            and end_time > :startTime
            and status = 'CONFIRMED'
    """
    )
    fun findBusyRoomIds(
        roomIds: Collection<UUID>,
        startTime: Instant,
        endTime: Instant,
    ): Set<UUID>
}
