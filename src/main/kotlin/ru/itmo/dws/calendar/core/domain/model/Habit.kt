package ru.itmo.dws.calendar.core.domain.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import ru.itmo.dws.calendar.core.domain.valueobject.BufferDuration
import ru.itmo.dws.calendar.core.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.RecurrenceRule
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

data class Habit(
    val id: HabitId,
    val userId: UserId,
    val title: String,
    val description: String? = null,
    val duration: Duration,
    val recurrenceRule: RecurrenceRule,
    val flexibilityWindow: HabitFlexibilityWindow,
    val priority: Priority = Priority.forHabit(),
    val currentTimeSlot: TimeSlot? = null,
    val bufferTime: BufferDuration = BufferDuration.NONE
) {
    init {
        require(title.isNotBlank()) { "Habit title cannot be blank" }
        require(!duration.isNegative && !duration.isZero) { "Habit duration must be positive" }
    }

    fun effectiveTimeSlot(): TimeSlot? {
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

    fun conflictsWith(timeSlot: TimeSlot): Boolean {
        val effectiveSlot = effectiveTimeSlot() ?: return false
        return effectiveSlot.overlapsWith(timeSlot)
    }

    fun conflictsWith(meeting: Meeting): Boolean {
        return conflictsWith(meeting.effectiveTimeSlot())
    }

    fun conflictsWith(focusTime: FocusTime): Boolean {
        return focusTime.userId == userId && conflictsWith(focusTime.timeSlot)
    }

    fun conflictsWith(other: Habit): Boolean {
        if (other.id == id || other.userId != userId) return false
        val otherSlot = other.effectiveTimeSlot() ?: return false
        return conflictsWith(otherSlot)
    }

    fun generatePossibleSlots(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
        interval: Duration = Duration.ofMinutes(15)
    ): List<TimeSlot> {
        if (!shouldOccurOn(date)) {
            return emptyList()
        }

        val slots = mutableListOf<TimeSlot>()
        var currentStart = ZonedDateTime.of(date, flexibilityWindow.earliestTime, zoneId)
        val latestEnd = ZonedDateTime.of(date, flexibilityWindow.latestTime, zoneId)

        while (true) {
            val slotEnd = currentStart.plus(duration)
            if (slotEnd.isAfter(latestEnd)) break

            val slot = TimeSlot(currentStart, slotEnd)
            if (isWithinFlexibilityWindow(slot)) {
                slots.add(slot)
            }
            currentStart = currentStart.plus(interval)
        }

        return slots
    }

    fun canMoveToDifferentDay(): Boolean = flexibilityWindow.allowCrossDayMove

    fun withPriority(newPriority: Priority): Habit = copy(priority = newPriority)

    fun withBufferTime(newBufferTime: BufferDuration): Habit = copy(bufferTime = newBufferTime)

    fun preferredStartTime(): LocalTime? = flexibilityWindow.preferredDuration?.let {
        flexibilityWindow.earliestTime
    }
}
