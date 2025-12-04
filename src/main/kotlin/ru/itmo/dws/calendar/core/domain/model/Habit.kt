package ru.itmo.dws.calendar.core.domain.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import ru.itmo.dws.calendar.core.domain.valueobject.BufferDuration
import ru.itmo.dws.calendar.core.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.RecurrenceRule
import ru.itmo.dws.calendar.core.domain.valueobject.SchedulingRule
import ru.itmo.dws.calendar.core.domain.valueobject.TimeRange
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

data class Habit(
    val id: HabitId,
    val userId: UserId,
    override val title: String,
    override val description: String? = null,
    val duration: Duration,
    val recurrenceRule: RecurrenceRule,
    val flexibilityWindow: HabitFlexibilityWindow,
    override val priority: Priority = Priority.forHabit(),
    val currentTimeSlot: TimeSlot? = null,
    val bufferTime: BufferDuration = BufferDuration.NONE,
    override val schedulingRules: List<SchedulingRule> = emptyList()
) : SchedulableEvent {

    override val eventId: String get() = id.toString()
    override val eventType: EventType get() = EventType.HABIT
    override val affectedUsers: List<UserId> get() = listOf(userId)

    init {
        require(title.isNotBlank()) { "Habit title cannot be blank" }
        require(!duration.isNegative && !duration.isZero) { "Habit duration must be positive" }
    }

    override fun effectiveTimeSlot(): TimeSlot? {
        return currentTimeSlot?.let {
            if (bufferTime.hasBuffer()) {
                it.withBuffer(bufferTime)
            } else {
                it
            }
        }
    }

    fun shouldOccurOn(date: LocalDate): Boolean {
        return recurrenceRule.occursOn(date)
    }

    fun isWithinFlexibilityWindow(timeSlot: TimeSlot): Boolean {
        val startTime = timeSlot.start.toLocalTime()
        val endTime = timeSlot.end.toLocalTime()

        return flexibilityWindow.isWithinWindow(startTime) &&
            flexibilityWindow.isWithinWindow(endTime)
    }

    fun reschedule(newTimeSlot: TimeSlot): Habit {
        require(isWithinFlexibilityWindow(newTimeSlot)) {
            "New time slot must be within flexibility window"
        }
        return copy(currentTimeSlot = newTimeSlot)
    }

    fun clearTimeSlot(): Habit {
        return copy(currentTimeSlot = null)
    }

    fun canMoveToDifferentDay(): Boolean = flexibilityWindow.allowCrossDayMove

    fun withPriority(newPriority: Priority): Habit = copy(priority = newPriority)

    fun withBufferTime(newBufferTime: BufferDuration): Habit = copy(bufferTime = newBufferTime)

    fun preferredStartTime(): LocalTime? = flexibilityWindow.preferredDuration?.let {
        flexibilityWindow.earliestTime
    }

    fun flexibilityTimeRange(): TimeRange = TimeRange(
        start = flexibilityWindow.earliestTime,
        end = flexibilityWindow.latestTime
    )
}
