package ru.itmo.dws.calendar.dto.room

import java.util.UUID

data class MeetingRoomResponseDto(
    val id: UUID,
    val name: String,
    val capacity: Int,
    val location: RoomLocationResponseDto,
    val status: MeetingRoomStatusDto,
    val features: Set<String>,
    val attributes: Map<String, String>,
)
