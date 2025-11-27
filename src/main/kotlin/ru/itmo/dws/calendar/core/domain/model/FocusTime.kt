package ru.itmo.dws.calendar.core.domain.model

import ru.itmo.dws.calendar.core.domain.valueobject.FocusTimeId
import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

data class FocusTime(
    val id: FocusTimeId,
    val userId: UserId,
    val timeSlot: TimeSlot,
    override val title: String = "Focus Time",
    override val description: String? = null,
    val isRecurring: Boolean = false
) : SchedulableEvent {

    override val eventId: String get() = id.toString()
    override val eventType: EventType get() = EventType.FOCUS_TIME
    override val priority: Priority get() = Priority.forFocusTime()
    override val affectedUsers: List<UserId> get() = listOf(userId)

    init {
        require(title.isNotBlank()) { "Focus time title cannot be blank" }
    }

    override fun effectiveTimeSlot(): TimeSlot = timeSlot

    fun contains(other: TimeSlot): Boolean {
        return timeSlot.contains(other)
    }
}
