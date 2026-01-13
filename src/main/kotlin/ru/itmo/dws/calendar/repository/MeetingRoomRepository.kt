package ru.itmo.dws.calendar.repository

import java.util.UUID
import org.springframework.data.repository.CrudRepository
import ru.itmo.dws.calendar.model.MeetingRoomEntity
import ru.itmo.dws.calendar.repository.extension.MeetingRoomRepositoryExtension

interface MeetingRoomRepository : MeetingRoomRepositoryExtension, CrudRepository<MeetingRoomEntity, UUID>
