package ru.itmo.dws.calendar.core.port.output.room

import ru.itmo.dws.calendar.core.domain.model.MeetingRoom
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomSearchCriteria

interface MeetingRoomProvider {
    fun findById(roomId: MeetingRoomId): MeetingRoom?

    fun findAllByCriteria(criteria: MeetingRoomSearchCriteria): List<MeetingRoom>
}
