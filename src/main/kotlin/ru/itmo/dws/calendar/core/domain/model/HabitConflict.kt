package ru.itmo.dws.calendar.core.domain.model

import java.time.Instant
import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

data class HabitConflict(
    val habitId: HabitId,
    val habitTitle: String,
    val habitTimeSlot: TimeSlot,
    val habitPriority: Priority,
    val conflictingEvent: ConflictingEvent,
    val conflictType: ConflictType,
    val affectedDate: LocalDate,
    val userId: UserId,
    val detectedAt: Instant = Instant.now()
) {
    val conflictingPriority: Priority
        get() = conflictingEvent.priority

    val isPriorityHigherThanConflicting: Boolean
        get() = habitPriority.isHigherThan(conflictingPriority)

    enum class ConflictType {
        MEETING_OVERLAP,
        FOCUS_TIME_OVERLAP,
        HABIT_OVERLAP
    }

    companion object {
        fun fromMeeting(habit: Habit, meeting: Meeting, date: LocalDate): HabitConflict? {
            val habitSlot = habit.effectiveTimeSlot() ?: return null
            if (!habitSlot.overlapsWith(meeting.effectiveTimeSlot())) return null

            return HabitConflict(
                habitId = habit.id,
                habitTitle = habit.title,
                habitTimeSlot = habitSlot,
                habitPriority = habit.priority,
                conflictingEvent = ConflictingEvent.MeetingEvent(meeting),
                conflictType = ConflictType.MEETING_OVERLAP,
                affectedDate = date,
                userId = habit.userId
            )
        }

        fun fromFocusTime(habit: Habit, focusTime: FocusTime, date: LocalDate): HabitConflict? {
            val habitSlot = habit.effectiveTimeSlot() ?: return null
            if (!habitSlot.overlapsWith(focusTime.timeSlot)) return null

            return HabitConflict(
                habitId = habit.id,
                habitTitle = habit.title,
                habitTimeSlot = habitSlot,
                habitPriority = habit.priority,
                conflictingEvent = ConflictingEvent.FocusTimeEvent(focusTime),
                conflictType = ConflictType.FOCUS_TIME_OVERLAP,
                affectedDate = date,
                userId = habit.userId
            )
        }

        fun fromHabit(habit: Habit, otherHabit: Habit, date: LocalDate): HabitConflict? {
            val habitSlot = habit.effectiveTimeSlot() ?: return null
            val otherSlot = otherHabit.effectiveTimeSlot() ?: return null
            if (!habitSlot.overlapsWith(otherSlot)) return null

            return HabitConflict(
                habitId = habit.id,
                habitTitle = habit.title,
                habitTimeSlot = habitSlot,
                habitPriority = habit.priority,
                conflictingEvent = ConflictingEvent.HabitEvent(otherHabit),
                conflictType = ConflictType.HABIT_OVERLAP,
                affectedDate = date,
                userId = habit.userId
            )
        }
    }
}

sealed class ConflictingEvent {
    abstract val priority: Priority
    abstract val timeSlot: TimeSlot
    abstract val title: String

    data class MeetingEvent(val meeting: Meeting) : ConflictingEvent() {
        override val priority: Priority get() = meeting.priority
        override val timeSlot: TimeSlot get() = meeting.effectiveTimeSlot()
        override val title: String get() = meeting.title
    }

    data class FocusTimeEvent(val focusTime: FocusTime) : ConflictingEvent() {
        override val priority: Priority get() = Priority.forFocusTime()
        override val timeSlot: TimeSlot get() = focusTime.timeSlot
        override val title: String get() = focusTime.title
    }

    data class HabitEvent(val habit: Habit) : ConflictingEvent() {
        override val priority: Priority get() = habit.priority
        override val timeSlot: TimeSlot get() = habit.effectiveTimeSlot()!!
        override val title: String get() = habit.title
    }
}
