package ru.itmo.dws.calendar.core.domain.valueobject

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class RecurrenceRule(
    val frequency: Frequency,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val interval: Int = 1,
    val startDate: LocalDate,
    val endDate: LocalDate? = null
) {
    init {
        require(interval > 0) { "Interval must be positive" }
        if (frequency == Frequency.WEEKLY) {
            require(daysOfWeek.isNotEmpty()) { "Days of week must be specified for weekly frequency" }
        }
        if (endDate != null) {
            require(!endDate.isBefore(startDate)) { "End date cannot be before start date" }
        }
    }

    fun occursOn(date: LocalDate): Boolean {
        if (date.isBefore(startDate)) {
            return false
        }
        if (endDate != null && date.isAfter(endDate)) {
            return false
        }

        return when (frequency) {
            Frequency.DAILY -> {
                val daysBetween = ChronoUnit.DAYS.between(startDate, date)
                daysBetween % interval == 0L
            }
            Frequency.WEEKLY -> {
                if (interval > 1) {
                    val daysBetween = ChronoUnit.DAYS.between(startDate, date)
                    val weeksBetween = daysBetween / 7
                    if (weeksBetween % interval != 0L) return false
                }
                daysOfWeek.contains(date.dayOfWeek)
            }
        }
    }

    enum class Frequency {
        DAILY,
        WEEKLY
    }

    companion object {
        fun daily(startDate: LocalDate, endDate: LocalDate? = null): RecurrenceRule {
            return RecurrenceRule(Frequency.DAILY, emptySet(), 1, startDate, endDate)
        }

        fun weekly(startDate: LocalDate, daysOfWeek: Set<DayOfWeek>, endDate: LocalDate? = null): RecurrenceRule {
            return RecurrenceRule(Frequency.WEEKLY, daysOfWeek, 1, startDate, endDate)
        }
    }
}
