package ru.itmo.dws.calendar.core.service.provider

import com.fasterxml.jackson.databind.ObjectMapper
import ru.itmo.dws.calendar.core.domain.model.MeetingRoom
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomSearchCriteria
import ru.itmo.dws.calendar.core.port.output.room.MeetingRoomProvider
import ru.itmo.dws.calendar.mapper.toDomain
import ru.itmo.dws.calendar.repository.MeetingRoomRepository

open class DatabaseMeetingRoomProvider(
    private val repository: MeetingRoomRepository,
    private val objectMapper: ObjectMapper,
) : MeetingRoomProvider {

    override fun findById(roomId: MeetingRoomId): MeetingRoom? {
        val entity = repository.findById(roomId.value).orElse(null)
        return entity?.toDomain(objectMapper)
    }

    override fun findAllByCriteria(criteria: MeetingRoomSearchCriteria): List<MeetingRoom> {
        return repository.findAllByCriteria(criteria).map { it.toDomain(objectMapper) }
    }
}
