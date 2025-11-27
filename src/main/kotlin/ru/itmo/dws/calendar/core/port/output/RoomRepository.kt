package ru.itmo.dws.calendar.core.port.output

import ru.itmo.dws.calendar.core.domain.model.Room
import ru.itmo.dws.calendar.core.domain.valueobject.RoomId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot

interface RoomRepository {

    fun findRoom(roomId: RoomId): Room?

    fun findAllRooms(): List<Room>

    fun findAvailableRooms(timeSlot: TimeSlot, minimumCapacity: Int): List<Room>

    fun checkRoomAvailability(roomId: RoomId, timeSlot: TimeSlot): Boolean

    fun bookRoom(roomId: RoomId, timeSlot: TimeSlot): Boolean

    fun cancelRoomBooking(roomId: RoomId, timeSlot: TimeSlot): Boolean
}
