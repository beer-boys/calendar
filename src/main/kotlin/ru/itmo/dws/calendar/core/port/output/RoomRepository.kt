package ru.itmo.dws.calendar.core.port.output

import ru.itmo.dws.calendar.core.domain.model.MeetingRoom
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId

interface RoomRepository {

    fun findRoom(roomId: MeetingRoomId): MeetingRoom?

    fun findAllRooms(): List<MeetingRoom>

    fun findAvailableRooms(timeSlot: TimeSlot, minimumCapacity: Int): List<MeetingRoom>

    fun checkRoomAvailability(roomId: MeetingRoomId, timeSlot: TimeSlot): Boolean

    fun bookRoom(roomId: MeetingRoomId, timeSlot: TimeSlot): Boolean

    fun cancelRoomBooking(roomId: MeetingRoomId, timeSlot: TimeSlot): Boolean
}
