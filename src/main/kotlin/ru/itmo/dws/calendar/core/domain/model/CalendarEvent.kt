package ru.itmo.dws.calendar.core.domain.model

import ru.itmo.dws.calendar.core.domain.valueobject.CalendarId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

data class CalendarEvent(
    val externalId: String,
    val calendarId: CalendarId,
    val owner: UserId,
    val timeSlot: TimeSlot,
    val title: String,
    val description: String? = null,
    val participants: List<UserId> = emptyList(),
    val eventType: EventType = EventType.REGULAR,
    val isAllDay: Boolean = false
) {
    init {
        require(externalId.isNotBlank()) { "External event ID cannot be blank" }
        require(title.isNotBlank()) { "Event title cannot be blank" }
    }

    fun conflictsWith(other: TimeSlot): Boolean {
        return timeSlot.overlapsWith(other)
    }

    fun conflictsWith(other: CalendarEvent): Boolean {
        return timeSlot.overlapsWith(other.timeSlot)
    }

    fun hasParticipant(userId: UserId): Boolean {
        return owner == userId || participants.contains(userId)
    }

    fun isBlocking(): Boolean {
        return eventType != EventType.POSSIBLE && eventType != EventType.FREE
    }

    enum class EventType {
        REGULAR,
        POSSIBLE,
        FREE
    }

    companion object {
        fun fromMeeting(meeting: Meeting, calendarId: CalendarId): CalendarEvent {
            return CalendarEvent(
                externalId = meeting.id.toString(),
                calendarId = calendarId,
                owner = meeting.creator,
                timeSlot = meeting.timeSlot,
                title = meeting.title,
                description = meeting.description,
                participants = meeting.participants,
                eventType = EventType.REGULAR
            )
        }

        fun fromHabit(habit: Habit, calendarId: CalendarId): CalendarEvent? {
            val timeSlot = habit.currentTimeSlot ?: return null

            return CalendarEvent(
                externalId = habit.id.toString(),
                calendarId = calendarId,
                owner = habit.userId,
                timeSlot = timeSlot,
                title = habit.title,
                description = habit.description,
                participants = listOf(habit.userId),
                eventType = EventType.REGULAR
            )
        }

        fun fromFocusTime(focusTime: FocusTime, calendarId: CalendarId): CalendarEvent {
            return CalendarEvent(
                externalId = focusTime.id.toString(),
                calendarId = calendarId,
                owner = focusTime.userId,
                timeSlot = focusTime.timeSlot,
                title = focusTime.title,
                description = focusTime.description,
                participants = listOf(focusTime.userId),
                eventType = EventType.REGULAR
            )
        }
    }
}
