package ru.itmo.dws.calendar.repository.extension

import ru.itmo.dws.calendar.model.MeetingRoomBookingEntity

interface MeetingRoomBookingRepositoryExtension {
    fun insert(booking: MeetingRoomBookingEntity): MeetingRoomBookingEntity
}
