package ru.itmo.dws.calendar.core.domain.model

import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.SchedulingRule
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface SchedulableEvent {
    val eventId: String
    val eventType: EventType
    val title: String
    val description: String?
    val priority: Priority
    val affectedUsers: List<UserId>

    val schedulingRules: List<SchedulingRule>
        get() = emptyList()

    fun effectiveTimeSlot(): TimeSlot?

    fun conflictsWith(timeSlot: TimeSlot): Boolean {
        val effective = effectiveTimeSlot() ?: return false
        return effective.overlapsWith(timeSlot)
    }

    fun conflictsWith(other: SchedulableEvent): Boolean {
        val otherSlot = other.effectiveTimeSlot() ?: return false
        return conflictsWith(otherSlot) && hasCommonUsers(other)
    }

    fun hasCommonUsers(other: SchedulableEvent): Boolean {
        return affectedUsers.any { other.affectedUsers.contains(it) }
    }

    fun affectsUser(userId: UserId): Boolean {
        return affectedUsers.contains(userId)
    }
}

enum class EventType {
    MEETING,
    HABIT,
    FOCUS_TIME,
    EXTERNAL_EVENT
}
