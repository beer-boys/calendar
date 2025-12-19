package ru.itmo.dws.calendar.core.service

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.model.HabitSchedulePlan
import ru.itmo.dws.calendar.core.domain.model.OccurrenceStatus
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.CalendarProvider
import ru.itmo.dws.calendar.core.service.provider.SchedulableEventProvider

class HabitSchedulingService(
    private val eventProviders: List<SchedulableEventProvider>,
    private val eventSlotFinder: EventSlotFinder,
    private val calendarProvider: CalendarProvider? = null,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun planSchedule(habit: Habit, weeks: Int = DEFAULT_PLANNING_WEEKS): HabitSchedulePlan {
        val today = LocalDate.now(zoneId)
        val periodEnd = today.plusWeeks(weeks.toLong())

        val occurrences = generateDateRange(today, periodEnd)
            .filter { habit.shouldOccurOn(it) }
            .map { date -> planOccurrenceForDate(habit, date) }
            .toList()

        return HabitSchedulePlan(
            habitId = habit.id,
            habitTitle = habit.title,
            periodStart = today,
            periodEnd = periodEnd,
            occurrences = occurrences
        )
    }

    private fun planOccurrenceForDate(habit: Habit, date: LocalDate): HabitOccurrence {
        val occupiedSlots = collectOccupiedSlotsForDate(habit.userId, date, habit.id.toString())

        val slot = eventSlotFinder.findOptimalSlot(
            event = habit,
            date = date,
            baseTimeWindow = habit.flexibilityTimeRange(),
            eventDuration = habit.duration,
            occupiedSlots = occupiedSlots,
            bufferTime = habit.bufferTime,
            preferredStartTime = habit.preferredStartTime(),
            zoneId = zoneId
        )

        return if (slot != null) {
            HabitOccurrence(
                habitId = habit.id,
                date = date,
                status = OccurrenceStatus.SCHEDULED,
                timeSlot = slot
            )
        } else {
            HabitOccurrence(
                habitId = habit.id,
                date = date,
                status = OccurrenceStatus.UNSCHEDULED,
                reason = "No available slot in flexibility window"
            )
        }
    }

    private fun collectOccupiedSlotsForDate(
        userId: UserId,
        date: LocalDate,
        excludeEventId: String
    ): List<TimeSlot> {
        val internalEvents = eventProviders.flatMap { provider ->
            provider.getEventsForUserOnDate(userId, date)
        }
        val internalSlots = eventSlotFinder.collectOccupiedSlots(internalEvents, excludeEventId)

        val externalSlots = fetchExternalCalendarSlots(userId, date)

        return internalSlots + externalSlots
    }

    private fun fetchExternalCalendarSlots(userId: UserId, date: LocalDate): List<TimeSlot> {
        if (calendarProvider == null) return emptyList()

        val dayStart = ZonedDateTime.of(date.atStartOfDay(), zoneId)
        val dayEnd = ZonedDateTime.of(date.plusDays(1).atStartOfDay(), zoneId)
        val dayRange = TimeSlot(dayStart, dayEnd)

        return try {
            calendarProvider.getEvents(userId, dayRange).map { it.timeSlot }
        } catch (_: RuntimeException) {
            emptyList()
        }
    }

    private fun generateDateRange(start: LocalDate, end: LocalDate): Sequence<LocalDate> {
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
    }

    companion object {
        const val DEFAULT_PLANNING_WEEKS = 4
    }
}
