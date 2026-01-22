package ru.itmo.dws.calendar.core.service.utils

import ru.itmo.dws.calendar.core.domain.model.CalendarEvent
import ru.itmo.dws.calendar.core.domain.model.EventType
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

class ExternalEventAdapter private constructor(
    private val calendarEvent: CalendarEvent
) : SchedulableEvent {
    override val eventId: String = calendarEvent.externalId
    override val eventType: EventType = EventType.EXTERNAL_EVENT
    override val title: String = calendarEvent.title
    override val description: String? = calendarEvent.description
    override val priority: Priority = Priority.NORMAL
    override val affectedUsers: List<UserId> = listOf(calendarEvent.owner) + calendarEvent.participants

    override fun effectiveTimeSlot(): TimeSlot = calendarEvent.timeSlot

    companion object {
        fun fromCalendarEvent(calendarEvent: CalendarEvent): ExternalEventAdapter {
            return ExternalEventAdapter(calendarEvent)
        }
    }
}
