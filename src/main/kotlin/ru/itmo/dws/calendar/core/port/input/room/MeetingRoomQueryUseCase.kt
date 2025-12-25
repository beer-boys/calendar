package ru.itmo.dws.calendar.core.port.input.room

import java.time.Duration
import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.model.MeetingRoom
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomSearchCriteria

interface MeetingRoomQueryUseCase {

    fun findRooms(criteria: MeetingRoomSearchCriteria): List<MeetingRoom>

    fun findAvailableSlots(
        roomId: MeetingRoomId,
        date: LocalDate,
        duration: Duration,
    ): List<TimeSlot>

    fun findAvailableRooms(
        timeSlot: TimeSlot,
        criteria: MeetingRoomSearchCriteria? = null,
    ): List<MeetingRoom>
}
