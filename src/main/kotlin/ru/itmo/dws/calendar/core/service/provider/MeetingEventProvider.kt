package ru.itmo.dws.calendar.core.service.provider

import java.time.LocalDate
import java.time.ZoneId
import ru.itmo.dws.calendar.core.domain.model.EventType
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.MeetingRepository

class MeetingEventProvider(
    private val meetingRepository: MeetingRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : SchedulableEventProvider {

    override val eventType: EventType = EventType.MEETING

    override fun getEventsForUser(userId: UserId, timeRange: TimeSlot): List<SchedulableEvent> {
        return meetingRepository.findMeetings(userId, timeRange)
    }

    override fun getEventsForUserOnDate(userId: UserId, date: LocalDate): List<SchedulableEvent> {
        val dayTimeSlot = createDayTimeSlot(date)
        return meetingRepository.findMeetings(userId, dayTimeSlot)
    }

    private fun createDayTimeSlot(date: LocalDate): TimeSlot {
        val startOfDay = date.atStartOfDay(zoneId)
        val endOfDay = date.plusDays(1).atStartOfDay(zoneId)
        return TimeSlot(startOfDay, endOfDay)
    }
}
