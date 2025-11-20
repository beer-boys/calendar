package ru.itmo.dws.calendar.domain.model

import ru.itmo.dws.calendar.domain.valueobject.BufferDuration
import ru.itmo.dws.calendar.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.domain.valueobject.HabitId
import ru.itmo.dws.calendar.domain.valueobject.RecurrenceRule
import ru.itmo.dws.calendar.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.domain.valueobject.UserId
import java.time.Duration
import java.time.LocalDate

data class Habit(
    val id: HabitId,
    val userId: UserId,
    val title: String,
    val description: String? = null,
    val duration: Duration,
    val recurrenceRule: RecurrenceRule,
    val flexibilityWindow: HabitFlexibilityWindow,
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

    fun conflictsWith(timeSlot: TimeSlot): Boolean {
        val effectiveSlot = effectiveTimeSlot() ?: return false
        return effectiveSlot.overlapsWith(timeSlot)
    }
}
