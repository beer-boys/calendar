package ru.itmo.dws.calendar.core.domain.valueobject

import java.time.DayOfWeek
import java.time.Duration

data class UserPreferences(
    val workingHours: WorkingHours = WorkingHours.default(),
    val preferredMeetingDuration: Duration = Duration.ofMinutes(30),
    val minimumBreakBetweenMeetings: Duration = Duration.ofMinutes(5),
    val preferredDaysForMeetings: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    ),
    val avoidEarlyMornings: Boolean = false,
    val avoidLateEvenings: Boolean = true,
    val maxMeetingsPerDay: Int = 8,
    val preferredConflictResolutionStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.FLEXIBILITY_FIRST,
    val autoRescheduleHabits: Boolean = true,
    val notificationPreferences: NotificationPreferences = NotificationPreferences()
) {
    companion object {
        fun default(): UserPreferences = UserPreferences()
    }
}

data class NotificationPreferences(
    val notifyOnMeetingRescheduled: Boolean = true,
    val notifyOnHabitMoved: Boolean = true,
    val notifyOnConflict: Boolean = true,
    val notifyBeforeMeeting: Boolean = true,
    val minutesBeforeMeeting: Int = 15
)
