package ru.itmo.dws.calendar.core.repository

import java.util.concurrent.ConcurrentHashMap
import ru.itmo.dws.calendar.core.domain.model.Meeting
import ru.itmo.dws.calendar.core.domain.valueobject.MeetingId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.MeetingRepository

open class InMemoryMeetingRepository : MeetingRepository {
    private val meetings = ConcurrentHashMap<MeetingId, Meeting>()

    override fun saveMeeting(meeting: Meeting): MeetingId {
        meetings[meeting.id] = meeting
        return meeting.id
    }

    override fun findMeeting(meetingId: MeetingId): Meeting? = meetings[meetingId]

    override fun findMeetings(userId: UserId, timeRange: TimeSlot): List<Meeting> =
        meetings.values.filter { meeting ->
            meeting.participants.contains(userId) &&
                meeting.timeSlot.overlapsWith(timeRange)
        }

    override fun updateMeeting(meetingId: MeetingId, meeting: Meeting): Boolean {
        if (meetings.containsKey(meetingId)) {
            meetings[meetingId] = meeting
            return true
        }
        return false
    }

    override fun deleteMeeting(meetingId: MeetingId): Boolean = meetings.remove(meetingId) != null
}
