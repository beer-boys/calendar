package ru.itmo.dws.calendar.core.service.provider

import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.model.EventType
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface SchedulableEventProvider {
    val eventType: EventType

    fun getEventsForUser(userId: UserId, timeRange: TimeSlot): List<SchedulableEvent>

    fun getEventsForUserOnDate(userId: UserId, date: LocalDate): List<SchedulableEvent>
}
