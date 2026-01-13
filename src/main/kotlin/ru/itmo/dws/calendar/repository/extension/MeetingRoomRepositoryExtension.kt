package ru.itmo.dws.calendar.repository.extension

import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomSearchCriteria
import ru.itmo.dws.calendar.model.MeetingRoomEntity

interface MeetingRoomRepositoryExtension {
    fun insert(room: MeetingRoomEntity): MeetingRoomEntity
    fun findAllByCriteria(criteria: MeetingRoomSearchCriteria): List<MeetingRoomEntity>
}
