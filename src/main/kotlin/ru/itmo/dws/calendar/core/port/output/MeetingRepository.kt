package ru.itmo.dws.calendar.core.port.output

import ru.itmo.dws.calendar.core.domain.model.Meeting
import ru.itmo.dws.calendar.core.domain.valueobject.MeetingId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface MeetingRepository {

    fun saveMeeting(meeting: Meeting): MeetingId

    fun findMeeting(meetingId: MeetingId): Meeting?

    fun findMeetings(userId: UserId, timeRange: TimeSlot): List<Meeting>

    fun updateMeeting(meetingId: MeetingId, meeting: Meeting): Boolean

    fun deleteMeeting(meetingId: MeetingId): Boolean
}
