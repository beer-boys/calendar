package ru.itmo.dws.calendar.repository.adapter

import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.core.domain.model.Meeting
import ru.itmo.dws.calendar.core.domain.valueobject.MeetingId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.MeetingRepository

@Component
class StubMeetingRepositoryAdapter : MeetingRepository {

    override fun saveMeeting(meeting: Meeting): MeetingId {
        throw UnsupportedOperationException("Meeting management not implemented in MVP")
    }

    override fun findMeeting(meetingId: MeetingId): Meeting? = null

    override fun findMeetings(userId: UserId, timeRange: TimeSlot): List<Meeting> = emptyList()

    override fun updateMeeting(meetingId: MeetingId, meeting: Meeting): Boolean {
        throw UnsupportedOperationException("Meeting management not implemented in MVP")
    }

    override fun deleteMeeting(meetingId: MeetingId): Boolean {
        throw UnsupportedOperationException("Meeting management not implemented in MVP")
    }
}
