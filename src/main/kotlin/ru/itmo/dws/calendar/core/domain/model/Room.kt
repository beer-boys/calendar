package ru.itmo.dws.calendar.core.domain.model

import ru.itmo.dws.calendar.core.domain.valueobject.RoomId

data class Room(
    val id: RoomId,
    val name: String,
    val capacity: Int,
    val location: String? = null
) {
    init {
        require(name.isNotBlank()) { "Room name cannot be blank" }
        require(capacity > 0) { "Room capacity must be positive" }
    }
}
