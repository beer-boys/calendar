package ru.itmo.dws.calendar.dto.room.booking

import java.util.UUID
import ru.itmo.dws.calendar.dto.TimeSlotDto

data class RoomBookingCreateDto(
    val roomId: String,
    val timeSlot: TimeSlotDto,
    val purpose: String,
    val sourceMeetingId: UUID? = null,
)
