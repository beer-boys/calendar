package ru.itmo.dws.calendar.domain.model

import java.time.Duration
import java.util.UUID
import ru.itmo.dws.calendar.domain.valueobject.*

data class MeetingDraft(
    val id: String = UUID.randomUUID().toString(),
    val creator: UserId,
    val participants: List<UserId>,
    val title: String,
    val description: String? = null,
    val duration: Duration,
    val preferredTimeRange: TimeSlot? = null,
    val room: RoomId? = null,
    val bufferTime: BufferDuration = BufferDuration.NONE,
    val constraints: SchedulingConstraints = SchedulingConstraints()
) {
    init {
        require(title.isNotBlank()) { "Meeting draft title cannot be blank" }
        require(participants.isNotEmpty()) { "Meeting draft must have at least one participant" }
        require(!duration.isNegative && !duration.isZero) { "Meeting duration must be positive" }
        require(participants.contains(creator)) { "Creator must be among participants" }
    }

    fun toMeeting(timeSlot: TimeSlot, meetingId: MeetingId = MeetingId.generate()): Meeting {
        require(timeSlot.duration() >= duration) {
            "Time slot duration must be at least as long as meeting duration"
        }

        return Meeting(
            id = meetingId,
            creator = creator,
            timeSlot = timeSlot,
            participants = participants,
            title = title,
            description = description,
            room = room,
            bufferTime = bufferTime
        )
    }

    fun participantCount(): Int = participants.size

    fun updateParticipants(newParticipants: List<UserId>): MeetingDraft {
        return copy(participants = newParticipants)
    }

    fun updateConstraints(newConstraints: SchedulingConstraints): MeetingDraft {
        return copy(constraints = newConstraints)
    }

    fun satisfies(timeSlot: TimeSlot): Boolean {
        if (timeSlot.duration() < duration) {
            return false
        }

        if (preferredTimeRange != null && !preferredTimeRange.contains(timeSlot)) {
            return false
        }

        return constraints.satisfies(timeSlot)
    }
}
