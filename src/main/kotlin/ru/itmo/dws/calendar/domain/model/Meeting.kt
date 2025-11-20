package ru.itmo.dws.calendar.domain.model

import ru.itmo.dws.calendar.domain.valueobject.BufferDuration
import ru.itmo.dws.calendar.domain.valueobject.MeetingId
import ru.itmo.dws.calendar.domain.valueobject.RoomId
import ru.itmo.dws.calendar.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.domain.valueobject.UserId

data class Meeting(
    val id: MeetingId,
    val creator: UserId,
    val timeSlot: TimeSlot,
    val participants: List<UserId>,
    val title: String,
    val description: String? = null,
    val room: RoomId? = null,
    val bufferTime: BufferDuration = BufferDuration.NONE
) {
    init {
        require(title.isNotBlank()) { "Meeting title cannot be blank" }
        require(participants.isNotEmpty()) { "Meeting must have at least one participant" }
        require(participants.contains(creator)) { "Creator must be among participants" }
    }

    fun effectiveTimeSlot(): TimeSlot {
        return if (bufferTime.hasBuffer()) {
            timeSlot.withBuffer(bufferTime)
        } else {
            timeSlot
        }
    }

    fun conflictsWith(other: TimeSlot): Boolean {
        return effectiveTimeSlot().overlapsWith(other)
    }

    fun conflictsWith(other: Meeting): Boolean {
        return effectiveTimeSlot().overlapsWith(other.effectiveTimeSlot())
    }

    fun reschedule(newTimeSlot: TimeSlot): Meeting {
        return copy(timeSlot = newTimeSlot)
    }

    fun updateParticipants(newParticipants: List<UserId>): Meeting {
        return copy(participants = newParticipants)
    }

    fun updateBufferTime(newBufferTime: BufferDuration): Meeting {
        return copy(bufferTime = newBufferTime)
    }

    fun hasParticipant(userId: UserId): Boolean {
        return participants.contains(userId)
    }

    fun participantCount(): Int = participants.size
}
