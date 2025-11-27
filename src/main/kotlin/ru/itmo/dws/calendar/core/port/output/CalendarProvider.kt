package ru.itmo.dws.calendar.core.port.output

import ru.itmo.dws.calendar.core.domain.model.CalendarEvent
import ru.itmo.dws.calendar.core.domain.model.Meeting
import ru.itmo.dws.calendar.core.domain.valueobject.CalendarId
import ru.itmo.dws.calendar.core.domain.valueobject.MeetingId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface CalendarProvider {

    fun getEvents(userId: UserId, timeRange: TimeSlot): List<CalendarEvent>

    fun getEventsForUsers(userIds: List<UserId>, timeRange: TimeSlot): Map<UserId, List<CalendarEvent>>

    fun createEvent(meeting: Meeting): MeetingId

    fun updateEvent(meetingId: MeetingId, meeting: Meeting): Boolean

    fun deleteEvent(meetingId: MeetingId): Boolean

    fun getCalendarId(userId: UserId): CalendarId
}
