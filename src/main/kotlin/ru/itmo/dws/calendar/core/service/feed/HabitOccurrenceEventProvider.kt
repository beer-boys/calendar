package ru.itmo.dws.calendar.core.service.feed

import java.time.ZoneId
import ru.itmo.dws.calendar.core.domain.model.CalendarFeedItem
import ru.itmo.dws.calendar.core.domain.model.CalendarItemType
import ru.itmo.dws.calendar.core.domain.model.EventSource
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.model.ItemCapabilities
import ru.itmo.dws.calendar.core.domain.model.ItemDetails
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.HabitOccurrenceRepository
import ru.itmo.dws.calendar.core.port.output.HabitRepository
import ru.itmo.dws.calendar.core.port.output.InternalEventProvider

class HabitOccurrenceEventProvider(
    private val habitRepository: HabitRepository,
    private val occurrenceRepository: HabitOccurrenceRepository,
    private val zoneId: ZoneId
) : InternalEventProvider {

    override fun eventType(): CalendarItemType = CalendarItemType.HABIT

    override fun getEvents(userId: UserId, timeRange: TimeSlot): List<CalendarFeedItem> {
        val startDate = timeRange.start.toLocalDate()
        val endDate = timeRange.end.toLocalDate()

        val occurrences = occurrenceRepository.findByUserIdAndDateRange(userId, startDate, endDate)

        val habitIds = occurrences.map { it.habitId }.distinct()
        val habitsById = habitRepository.findByIds(habitIds).associateBy { it.id }

        return occurrences.mapNotNull { occurrence ->
            val habit = habitsById[occurrence.habitId] ?: return@mapNotNull null
            toCalendarFeedItem(habit, occurrence)
        }
    }

    private fun toCalendarFeedItem(habit: Habit, occurrence: HabitOccurrence): CalendarFeedItem? {
        val timeSlot = occurrence.timeSlot ?: return null

        val source = if (occurrence.externalEventId != null) {
            EventSource.MIRRORED
        } else {
            EventSource.INTERNAL_ONLY
        }

        return CalendarFeedItem(
            id = "${habit.id.value}_${occurrence.date}",
            timeSlot = timeSlot,
            title = habit.title,
            description = habit.description,
            itemType = CalendarItemType.HABIT,
            source = source,
            externalEventId = occurrence.externalEventId,
            capabilities = ItemCapabilities.forHabitOccurrence(),
            conflict = null,
            details = ItemDetails.Habit(
                habitId = habit.id,
                occurrenceDate = occurrence.date,
                occurrenceStatus = occurrence.status,
                flexibilityWindow = habit.flexibilityWindow,
                duration = habit.duration
            )
        )
    }
}
