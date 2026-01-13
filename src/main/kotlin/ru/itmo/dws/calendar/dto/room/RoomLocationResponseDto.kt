package ru.itmo.dws.calendar.dto.room

data class RoomLocationResponseDto(
    val address: String?,
    val building: String?,
    val floor: Int?,
    val wing: String?,
    val roomNumber: String?,
    val city: String?,
    val timeZoneId: String?
)
