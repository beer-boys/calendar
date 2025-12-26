package ru.itmo.dws.calendar.core.domain.model

import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId
import ru.itmo.dws.calendar.core.domain.valueobject.room.RoomFeatures
import ru.itmo.dws.calendar.core.domain.valueobject.room.RoomLocation

data class MeetingRoom(
    val id: MeetingRoomId,
    val name: String,
    val capacity: Int,
    val location: RoomLocation = RoomLocation(),
    val features: RoomFeatures = RoomFeatures.EMPTY,
    val status: MeetingRoomStatus = MeetingRoomStatus.ACTIVE,
) {

    enum class MeetingRoomStatus {
        ACTIVE,
        INACTIVE,
        MAINTENANCE,
        ;

        companion object {
            fun fromString(value: String): MeetingRoomStatus? {
                return entries.firstOrNull { value.uppercase() == it.name }
            }
        }
    }

    init {
        require(name.isNotBlank()) { "Room name cannot be blank" }
        require(capacity > 0) { "Room capacity must be positive" }
    }
}
