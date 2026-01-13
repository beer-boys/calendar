package ru.itmo.dws.calendar.dto.room.booking

import java.util.UUID
import ru.itmo.dws.calendar.dto.TimeSlotDto

data class RoomBookingResponseDto(
    val id: UUID,
    val roomId: UUID,
    val timeSlot: TimeSlotDto,
    val organizerId: UUID,
    val purpose: String? = null,
    val status: BookingStatusDto = BookingStatusDto.CONFIRMED,
)
