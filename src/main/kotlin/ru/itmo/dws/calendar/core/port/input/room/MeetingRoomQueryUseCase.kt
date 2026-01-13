package ru.itmo.dws.calendar.core.port.input.room

import java.time.Duration
import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.model.MeetingRoom
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomSearchCriteria

interface MeetingRoomQueryUseCase {

    fun findRooms(criteria: MeetingRoomSearchCriteria, userId: UserId): List<MeetingRoom>

    fun findAvailableSlots(
        roomId: MeetingRoomId,
        date: LocalDate,
        duration: Duration,
        userId: UserId,
    ): List<TimeSlot>

    fun findAvailableRooms(
        timeSlot: TimeSlot,
        criteria: MeetingRoomSearchCriteria? = null,
        userId: UserId,
    ): List<MeetingRoom>
}
