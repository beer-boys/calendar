package ru.itmo.dws.calendar.core.domain.model

import ru.itmo.dws.calendar.core.domain.valueobject.BufferDuration
import ru.itmo.dws.calendar.core.domain.valueobject.MeetingId
import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId

data class Meeting(
    val id: MeetingId,
    val creator: UserId,
    val timeSlot: TimeSlot,
    val participants: List<UserId>,
    override val title: String,
    override val description: String? = null,
    val room: MeetingRoomId? = null,
    override val priority: Priority = Priority.forMeeting(),
    val bufferTime: BufferDuration = BufferDuration.NONE
) : SchedulableEvent {

    override val eventId: String get() = id.toString()
    override val eventType: EventType get() = EventType.MEETING
    override val affectedUsers: List<UserId> get() = participants

    init {
        require(title.isNotBlank()) { "Meeting title cannot be blank" }
        require(participants.isNotEmpty()) { "Meeting must have at least one participant" }
        require(participants.contains(creator)) { "Creator must be among participants" }
    }

    override fun effectiveTimeSlot(): TimeSlot {
        return if (bufferTime.hasBuffer()) {
            timeSlot.withBuffer(bufferTime)
        } else {
            timeSlot
        }
    }
}
