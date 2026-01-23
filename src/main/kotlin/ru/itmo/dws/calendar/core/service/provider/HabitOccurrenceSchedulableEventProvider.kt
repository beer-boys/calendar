package ru.itmo.dws.calendar.core.service.provider

import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.model.EventType
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.HabitOccurrenceRepository
import ru.itmo.dws.calendar.core.port.output.HabitRepository

class HabitOccurrenceSchedulableEventProvider(
    private val habitOccurrenceRepository: HabitOccurrenceRepository,
    private val habitRepository: HabitRepository
) : SchedulableEventProvider {

    override val eventType: EventType = EventType.HABIT

    override fun getEventsForUser(userId: UserId, timeRange: TimeSlot): List<SchedulableEvent> {
        val startDate = timeRange.start.toLocalDate()
        val endDate = timeRange.end.toLocalDate()

        val occurrences = habitOccurrenceRepository.findByUserIdAndDateRange(userId, startDate, endDate)
        val habitIds = occurrences.map { it.habitId }.distinct()
        val habitsById = habitRepository.findByIds(habitIds).associateBy { it.id }

        return occurrences.mapNotNull { occurrence ->
            val habit = habitsById[occurrence.habitId] ?: return@mapNotNull null
            val occurrenceTimeSlot = occurrence.timeSlot ?: return@mapNotNull null

            if (!occurrenceTimeSlot.overlapsWith(timeRange)) {
                return@mapNotNull null
            }

            habit.copy(currentTimeSlot = occurrenceTimeSlot)
        }
    }

    override fun getEventsForUserOnDate(userId: UserId, date: LocalDate): List<SchedulableEvent> {
        val occurrences = habitOccurrenceRepository.findByUserIdAndDateRange(userId, date, date)
        val habitIds = occurrences.map { it.habitId }.distinct()
        val habitsById = habitRepository.findByIds(habitIds).associateBy { it.id }

        return occurrences.mapNotNull { occurrence ->
            val habit = habitsById[occurrence.habitId] ?: return@mapNotNull null
            val occurrenceTimeSlot = occurrence.timeSlot ?: return@mapNotNull null

            habit.copy(currentTimeSlot = occurrenceTimeSlot)
        }
    }
}
