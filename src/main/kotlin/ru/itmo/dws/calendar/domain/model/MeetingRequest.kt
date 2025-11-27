package ru.itmo.dws.calendar.domain.model

import java.time.Duration
import ru.itmo.dws.calendar.domain.valueobject.BufferDuration
import ru.itmo.dws.calendar.domain.valueobject.MeetingPriority
import ru.itmo.dws.calendar.domain.valueobject.RoomId
import ru.itmo.dws.calendar.domain.valueobject.SchedulingConstraints
import ru.itmo.dws.calendar.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.domain.valueobject.UserId

data class MeetingRequest(
    val creator: UserId,
    val participants: List<UserId>,
    val title: String,
    val description: String? = null,
    val duration: Duration,
    val preferredTimeSlot: TimeSlot? = null,
    val preferredTimeRange: TimeSlot? = null,
    val room: RoomId? = null,
    val bufferTime: BufferDuration = BufferDuration.NONE,
    val constraints: SchedulingConstraints = SchedulingConstraints(),
    val priority: MeetingPriority = MeetingPriority.NORMAL
) {
    fun toDraft(): MeetingDraft {
        return MeetingDraft(
            creator = creator,
            participants = participants,
            title = title,
            description = description,
            duration = duration,
            preferredTimeRange = preferredTimeRange,
            room = room,
            bufferTime = bufferTime,
            constraints = constraints
        )
    }
}
