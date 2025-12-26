package ru.itmo.dws.calendar.dto.room

import ru.itmo.dws.calendar.core.domain.model.MeetingRoom

enum class MeetingRoomStatusDto {
    ACTIVE,
    INACTIVE,
    MAINTENANCE,
    ;

    companion object {
        fun fromEntity(value: MeetingRoom.MeetingRoomStatus): MeetingRoomStatusDto {
            return when (value) {
                MeetingRoom.MeetingRoomStatus.ACTIVE -> ACTIVE
                MeetingRoom.MeetingRoomStatus.INACTIVE -> INACTIVE
                MeetingRoom.MeetingRoomStatus.MAINTENANCE -> MAINTENANCE
            }
        }
    }
}
