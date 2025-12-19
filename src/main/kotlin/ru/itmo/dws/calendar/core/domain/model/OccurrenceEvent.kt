package ru.itmo.dws.calendar.core.domain.model

import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.SchedulingRule
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

class OccurrenceEvent(
    private val habit: Habit,
    private val occurrence: HabitOccurrence
) : SchedulableEvent {

    override val eventId: String
        get() = "${habit.id.value}_${occurrence.date}"

    override val eventType: EventType
        get() = EventType.HABIT

    override val title: String
        get() = habit.title

    override val description: String?
        get() = buildDescription()

    override val priority: Priority
        get() = habit.priority

    override val affectedUsers: List<UserId>
        get() = listOf(habit.userId)

    override val schedulingRules: List<SchedulingRule>
        get() = habit.schedulingRules

    override fun effectiveTimeSlot(): TimeSlot? = occurrence.timeSlot

    val habitId: String get() = habit.id.value.toString()

    val occurrenceDate: String get() = occurrence.date.toString()

    val sourceApplication: String get() = SOURCE_APPLICATION

    private fun buildDescription(): String {
        val baseDescription = habit.description ?: ""
        return """
            |$baseDescription
            |
            |---
            |$METADATA_PREFIX
            |habitId: ${habit.id.value}
            |occurrenceDate: ${occurrence.date}
            |source: $SOURCE_APPLICATION
        """.trimMargin()
    }

    companion object {
        const val SOURCE_APPLICATION = "smart-calendar"
        const val METADATA_PREFIX = "[SmartCalendar Metadata]"
    }
}
