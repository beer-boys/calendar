package ru.itmo.dws.calendar.domain.valueobject

import java.time.DayOfWeek
import java.time.LocalDate

data class RecurrenceRule(
    val frequency: Frequency,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val interval: Int = 1,
    val endDate: LocalDate? = null
) {
    init {
        require(interval > 0) { "Interval must be positive" }
        if (frequency == Frequency.WEEKLY) {
            require(daysOfWeek.isNotEmpty()) { "Days of week must be specified for weekly frequency" }
        }
    }

    fun occursOn(date: LocalDate): Boolean {
        if (endDate != null && date.isAfter(endDate)) {
            return false
        }

        return when (frequency) {
            Frequency.DAILY -> true
            Frequency.WEEKLY -> daysOfWeek.contains(date.dayOfWeek)
        }
    }

    enum class Frequency {
        DAILY,
        WEEKLY
    }

    companion object {
        fun daily(endDate: LocalDate? = null): RecurrenceRule {
            return RecurrenceRule(Frequency.DAILY, emptySet(), 1, endDate)
        }

        fun weekly(daysOfWeek: Set<DayOfWeek>, endDate: LocalDate? = null): RecurrenceRule {
            return RecurrenceRule(Frequency.WEEKLY, daysOfWeek, 1, endDate)
        }
    }
}
