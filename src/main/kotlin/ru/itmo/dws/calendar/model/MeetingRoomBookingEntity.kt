package ru.itmo.dws.calendar.model

import java.time.Instant
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("meeting_room_bookings")
data class MeetingRoomBookingEntity(
    @Id
    val id: UUID,
    @Column("room_id")
    val roomId: UUID,
    @Column("organizer_id")
    val organizerId: UUID,
    val purpose: String?,
    val status: String,

    @Column("start_time")
    val startTime: Instant,
    @Column("end_time")
    val endTime: Instant,
)
