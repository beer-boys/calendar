package ru.itmo.dws.calendar.core.domain.valueobject

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

sealed interface SchedulingRule {
    fun applyTo(context: SchedulingContext): SchedulingContext

    data class TimeWindowRule(
        val earliestTime: LocalTime,
        val latestTime: LocalTime,
        val activeDateRange: DateRange? = null,
        val activeDaysOfWeek: Set<DayOfWeek>? = null
    ) : SchedulingRule {
        init {
            require(latestTime.isAfter(earliestTime)) {
                "Latest time must be after earliest time"
            }
        }

        override fun applyTo(context: SchedulingContext): SchedulingContext {
            if (!isActiveOn(context.date)) return context

            val newWindow = TimeRange(earliestTime, latestTime)
            return context.withNarrowedTimeWindow(newWindow)
        }

        fun isActiveOn(date: LocalDate): Boolean {
            if (activeDateRange != null && !activeDateRange.contains(date)) return false
            if (activeDaysOfWeek != null && !activeDaysOfWeek.contains(date.dayOfWeek)) return false
            return true
        }

        fun containsTime(time: LocalTime): Boolean {
            return !time.isBefore(earliestTime) && !time.isAfter(latestTime)
        }

        fun windowDuration(): Duration = Duration.between(earliestTime, latestTime)
    }

    data class ExclusionRule(
        val excludedDates: Set<LocalDate> = emptySet(),
        val excludedDaysOfWeek: Set<DayOfWeek> = emptySet(),
        val excludedTimeRanges: List<DayTimeExclusion> = emptyList(),
        val excludeHolidays: Boolean = false
    ) : SchedulingRule {

        override fun applyTo(context: SchedulingContext): SchedulingContext {
            if (isDateFullyExcluded(context.date)) {
                return context.withDateExcluded("Date is in exclusion list")
            }

            val dayExclusion = excludedTimeRanges.find { it.dayOfWeek == context.date.dayOfWeek }
            if (dayExclusion != null) {
                if (dayExclusion.isWholeDayExcluded()) {
                    return context.withDateExcluded("Entire day of week is excluded")
                }
                return context.withExcludedRanges(dayExclusion.excludedRanges)
            }

            return context
        }

        fun isExcluded(date: LocalDate, time: LocalTime? = null): Boolean {
            if (excludedDates.contains(date)) return true
            if (excludedDaysOfWeek.contains(date.dayOfWeek)) return true

            if (time != null) {
                val dayExclusion = excludedTimeRanges.find { it.dayOfWeek == date.dayOfWeek }
                if (dayExclusion?.isTimeExcluded(time) == true) return true
            }

            return false
        }

        fun isDateFullyExcluded(date: LocalDate): Boolean {
            if (excludedDates.contains(date)) return true
            if (excludedDaysOfWeek.contains(date.dayOfWeek)) return true
            return false
        }
    }

    data class FrequencyRule(
        val period: Duration,
        val minOccurrences: Int? = null,
        val maxOccurrences: Int? = null,
        val minGapBetweenOccurrences: Duration? = null
    ) : SchedulingRule {
        init {
            require(!period.isNegative && !period.isZero) {
                "Period must be positive"
            }
            minOccurrences?.let { require(it > 0) { "Min occurrences must be positive" } }
            maxOccurrences?.let { require(it > 0) { "Max occurrences must be positive" } }
            if (minOccurrences != null && maxOccurrences != null) {
                require(maxOccurrences >= minOccurrences) {
                    "Max occurrences must be >= min occurrences"
                }
            }
        }

        override fun applyTo(context: SchedulingContext): SchedulingContext {
            return context.withFrequencyConstraint(
                FrequencyConstraint(
                    period = period,
                    minOccurrences = minOccurrences,
                    maxOccurrences = maxOccurrences,
                    minGapBetweenOccurrences = minGapBetweenOccurrences
                )
            )
        }

        fun periodDays(): Long = period.toDays()
    }

    data class RecurrenceExceptionRule(
        val cancelledDates: Set<LocalDate> = emptySet(),
        val modifiedOccurrences: Map<LocalDate, TimeSlotOverride> = emptyMap()
    ) : SchedulingRule {

        override fun applyTo(context: SchedulingContext): SchedulingContext {
            if (isCancelled(context.date)) {
                return context.withDateExcluded("Date is cancelled by recurrence exception")
            }

            val override = getModifiedTimeSlot(context.date)
            if (override != null) {
                return context.withTimeSlotOverride(override)
            }

            return context
        }

        fun isCancelled(date: LocalDate): Boolean = cancelledDates.contains(date)

        fun getModifiedTimeSlot(date: LocalDate): TimeSlotOverride? = modifiedOccurrences[date]

        fun hasExceptionOn(date: LocalDate): Boolean {
            return cancelledDates.contains(date) || modifiedOccurrences.containsKey(date)
        }
    }
}

data class DateRange(
    val start: LocalDate? = null,
    val end: LocalDate? = null
) {
    init {
        if (start != null && end != null) {
            require(!end.isBefore(start)) { "End date must be >= start date" }
        }
    }

    fun contains(date: LocalDate): Boolean {
        if (start != null && date.isBefore(start)) return false
        if (end != null && date.isAfter(end)) return false
        return true
    }

    companion object {
        fun from(start: LocalDate) = DateRange(start = start)
        fun until(end: LocalDate) = DateRange(end = end)
        fun between(start: LocalDate, end: LocalDate) = DateRange(start, end)
    }
}

data class DayTimeExclusion(
    val dayOfWeek: DayOfWeek,
    val excludedRanges: List<TimeRange> = emptyList()
) {
    fun isTimeExcluded(time: LocalTime): Boolean {
        if (excludedRanges.isEmpty()) return true
        return excludedRanges.any { it.contains(time) }
    }

    fun isWholeDayExcluded(): Boolean = excludedRanges.isEmpty()
}

data class TimeRange(
    val start: LocalTime,
    val end: LocalTime
) {
    init {
        require(end.isAfter(start)) { "End time must be after start time" }
    }

    fun contains(time: LocalTime): Boolean {
        return !time.isBefore(start) && !time.isAfter(end)
    }

    fun overlapsWith(other: TimeRange): Boolean {
        return start < other.end && end > other.start
    }

    fun duration(): Duration = Duration.between(start, end)
}

data class TimeSlotOverride(
    val newStartTime: LocalTime? = null,
    val newEndTime: LocalTime? = null,
    val newDuration: Duration? = null
)
