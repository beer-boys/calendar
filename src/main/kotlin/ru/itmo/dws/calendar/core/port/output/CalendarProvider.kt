package ru.itmo.dws.calendar.core.port.output

import ru.itmo.dws.calendar.core.domain.model.CalendarEvent
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface CalendarProvider {

    fun getEvents(userId: UserId, timeRange: TimeSlot): List<CalendarEvent>

    fun getEventsForUsers(userIds: List<UserId>, timeRange: TimeSlot): Map<UserId, List<CalendarEvent>>

    fun createEvent(userId: UserId, event: SchedulableEvent): String

    fun createRecurringEvent(userId: UserId, habit: Habit): String

    fun updateEvent(userId: UserId, externalEventId: String, event: SchedulableEvent): Boolean

    fun updateRecurringEvent(userId: UserId, externalEventId: String, habit: Habit): Boolean

    fun deleteEvent(userId: UserId, externalEventId: String): Boolean
}
