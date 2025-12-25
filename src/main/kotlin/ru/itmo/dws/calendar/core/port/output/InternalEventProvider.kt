package ru.itmo.dws.calendar.core.port.output

import ru.itmo.dws.calendar.core.domain.model.CalendarFeedItem
import ru.itmo.dws.calendar.core.domain.model.CalendarItemType
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface InternalEventProvider {

    fun eventType(): CalendarItemType

    fun getEvents(userId: UserId, timeRange: TimeSlot): List<CalendarFeedItem>
}
