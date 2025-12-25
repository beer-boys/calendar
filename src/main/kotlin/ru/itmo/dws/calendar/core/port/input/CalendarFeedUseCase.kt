package ru.itmo.dws.calendar.core.port.input

import ru.itmo.dws.calendar.core.domain.model.CalendarFeedItem
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface CalendarFeedUseCase {
    fun getCalendarFeed(userId: UserId, timeRange: TimeSlot): CalendarFeedResult
}

data class CalendarFeedResult(
    val events: List<CalendarFeedItem>,
    val period: TimeSlot,
    val totalCount: Int,
    val hasConflicts: Boolean
) {
    companion object {
        fun of(events: List<CalendarFeedItem>, period: TimeSlot): CalendarFeedResult {
            return CalendarFeedResult(
                events = events,
                period = period,
                totalCount = events.size,
                hasConflicts = events.any { it.conflict != null }
            )
        }
    }
}
