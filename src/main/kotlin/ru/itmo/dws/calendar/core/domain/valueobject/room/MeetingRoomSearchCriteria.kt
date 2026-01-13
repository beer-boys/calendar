package ru.itmo.dws.calendar.core.domain.valueobject.room

import ru.itmo.dws.calendar.core.domain.model.MeetingRoom.MeetingRoomStatus

data class MeetingRoomSearchCriteria(
    val minCapacity: Int? = null,
    val locationQuery: String? = null,
    val floor: Int? = null,
    val building: String? = null,
    val requiredFeatures: Set<RoomFeature> = emptySet(),
    val status: MeetingRoomStatus? = MeetingRoomStatus.ACTIVE
) {
    init {
        require(minCapacity == null || minCapacity > 0) { "minCapacity must be positive" }
    }
}
