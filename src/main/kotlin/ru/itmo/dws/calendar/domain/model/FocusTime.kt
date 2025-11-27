package ru.itmo.dws.calendar.domain.model

import ru.itmo.dws.calendar.domain.valueobject.FocusTimeId
import ru.itmo.dws.calendar.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.domain.valueobject.UserId

data class FocusTime(
    val id: FocusTimeId,
    val userId: UserId,
    val timeSlot: TimeSlot,
    val title: String = "Focus Time",
    val description: String? = null,
    val isRecurring: Boolean = false
) {
    init {
        require(title.isNotBlank()) { "Focus time title cannot be blank" }
    }

    fun conflictsWith(other: TimeSlot): Boolean {
        return timeSlot.overlapsWith(other)
    }

    fun conflictsWith(meeting: Meeting): Boolean {
        return meeting.hasParticipant(userId) && timeSlot.overlapsWith(meeting.effectiveTimeSlot())
    }

    fun reschedule(newTimeSlot: TimeSlot): FocusTime {
        return copy(timeSlot = newTimeSlot)
    }

    fun contains(other: TimeSlot): Boolean {
        return timeSlot.contains(other)
    }
}
