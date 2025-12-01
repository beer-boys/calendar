package ru.itmo.dws.calendar.core.domain.valueobject

import java.time.Duration
import java.time.LocalTime

data class HabitFlexibilityWindow(
    val earliestTime: LocalTime,
    val latestTime: LocalTime,
    val allowCrossDayMove: Boolean = false,
    val preferredDuration: Duration? = null
) {
    init {
        require(latestTime.isAfter(earliestTime)) {
            "Latest time must be after earliest time. Earliest: $earliestTime, Latest: $latestTime"
        }
    }

    fun isWithinWindow(time: LocalTime): Boolean {
        return !time.isBefore(earliestTime) && !time.isAfter(latestTime)
    }

    fun windowDuration(): Duration {
        return Duration.between(earliestTime, latestTime)
    }

    companion object {
        fun allDay(allowCrossDayMove: Boolean = false): HabitFlexibilityWindow {
            return HabitFlexibilityWindow(
                earliestTime = LocalTime.MIN,
                latestTime = LocalTime.MAX,
                allowCrossDayMove = allowCrossDayMove
            )
        }

        fun workingHours(allowCrossDayMove: Boolean = false): HabitFlexibilityWindow {
            return HabitFlexibilityWindow(
                earliestTime = LocalTime.of(9, 0),
                latestTime = LocalTime.of(18, 0),
                allowCrossDayMove = allowCrossDayMove
            )
        }

        fun morning(allowCrossDayMove: Boolean = false): HabitFlexibilityWindow {
            return HabitFlexibilityWindow(
                earliestTime = LocalTime.of(6, 0),
                latestTime = LocalTime.of(12, 0),
                allowCrossDayMove = allowCrossDayMove
            )
        }

        fun day(allowCrossDayMove: Boolean = false): HabitFlexibilityWindow {
            return HabitFlexibilityWindow(
                earliestTime = LocalTime.of(12, 0),
                latestTime = LocalTime.of(18, 0),
                allowCrossDayMove = allowCrossDayMove
            )
        }

        fun evening(allowCrossDayMove: Boolean = false): HabitFlexibilityWindow {
            return HabitFlexibilityWindow(
                earliestTime = LocalTime.of(18, 0),
                latestTime = LocalTime.of(22, 0),
                allowCrossDayMove = allowCrossDayMove
            )
        }
    }
}
