package ru.itmo.dws.calendar.domain.valueobject

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime

data class SchedulingConstraints(
    val preferredTimeRange: TimeSlot? = null,
    val workingHours: WorkingHours = WorkingHours.default(),
    val excludedDays: Set<DayOfWeek> = emptySet(),
    val minimumNoticePeriod: Duration = Duration.ZERO,
    val requiredRoomCapacity: Int? = null,
    val maxSuggestionsCount: Int = 10
) {
    fun satisfies(timeSlot: TimeSlot): Boolean {
        if (preferredTimeRange != null && !preferredTimeRange.contains(timeSlot)) {
            return false
        }

        if (excludedDays.contains(timeSlot.start.dayOfWeek)) {
            return false
        }

        if (!workingHours.isWithinWorkingHours(timeSlot)) {
            return false
        }

        return true
    }
}

data class WorkingHours(
    val start: LocalTime,
    val end: LocalTime,
    val workingDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )
) {
    init {
        require(end.isAfter(start)) {
            "End time must be after start time. Start: $start, End: $end"
        }
    }

    fun isWithinWorkingHours(timeSlot: TimeSlot): Boolean {
        val dayOfWeek = timeSlot.start.dayOfWeek
        if (!workingDays.contains(dayOfWeek)) {
            return false
        }

        val startTime = timeSlot.start.toLocalTime()
        val endTime = timeSlot.end.toLocalTime()

        return !startTime.isBefore(start) && !endTime.isAfter(end)
    }

    companion object {
        fun default(): WorkingHours {
            return WorkingHours(
                start = LocalTime.of(9, 0),
                end = LocalTime.of(18, 0)
            )
        }

        fun allTime(): WorkingHours {
            return WorkingHours(
                start = LocalTime.MIN,
                end = LocalTime.MAX,
                workingDays = DayOfWeek.entries.toSet()
            )
        }
    }
}
