package ru.itmo.dws.calendar.core.service.provider

import java.time.LocalDate
import java.time.ZoneId
import ru.itmo.dws.calendar.core.domain.model.EventType
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.HabitRepository

class HabitEventProvider(
    private val habitRepository: HabitRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : SchedulableEventProvider {

    override val eventType: EventType = EventType.HABIT

    override fun getEventsForUser(userId: UserId, timeRange: TimeSlot): List<SchedulableEvent> {
        val date = timeRange.start.toLocalDate()
        return habitRepository.findHabitsForDate(userId, date)
            .filter { habit ->
                habit.effectiveTimeSlot()?.overlapsWith(timeRange) == true
            }
    }

    override fun getEventsForUserOnDate(userId: UserId, date: LocalDate): List<SchedulableEvent> {
        return habitRepository.findHabitsForDate(userId, date)
    }
}
