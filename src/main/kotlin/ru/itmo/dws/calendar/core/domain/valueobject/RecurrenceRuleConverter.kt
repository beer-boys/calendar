package ru.itmo.dws.calendar.core.domain.valueobject

import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

object RecurrenceRuleConverter {
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

    private val DAY_OF_WEEK_TO_ICAL = mapOf(
        DayOfWeek.MONDAY to "MO",
        DayOfWeek.TUESDAY to "TU",
        DayOfWeek.WEDNESDAY to "WE",
        DayOfWeek.THURSDAY to "TH",
        DayOfWeek.FRIDAY to "FR",
        DayOfWeek.SATURDAY to "SA",
        DayOfWeek.SUNDAY to "SU"
    )

    private val ICAL_TO_DAY_OF_WEEK = DAY_OF_WEEK_TO_ICAL.entries.associate { (k, v) -> v to k }

    fun toRRule(rule: RecurrenceRule): String {
        val parts = mutableListOf<String>()

        parts.add("FREQ=${rule.frequency.name}")

        if (rule.interval > 1) {
            parts.add("INTERVAL=${rule.interval}")
        }

        if (rule.frequency == RecurrenceRule.Frequency.WEEKLY && rule.daysOfWeek.isNotEmpty()) {
            val days = rule.daysOfWeek
                .sortedBy { it.value }
                .mapNotNull { DAY_OF_WEEK_TO_ICAL[it] }
                .joinToString(",")
            parts.add("BYDAY=$days")
        }

        rule.endDate?.let {
            parts.add("UNTIL=${it.format(DATE_FORMATTER)}")
        }

        return "RRULE:${parts.joinToString(";")}"
    }

    fun toRRuleList(rule: RecurrenceRule): List<String> {
        return listOf(toRRule(rule))
    }

    fun fromRRule(rrule: String, startDate: java.time.LocalDate): RecurrenceRule? {
        val normalized = rrule.removePrefix("RRULE:")
        val parts = normalized.split(";").associate { part ->
            val (key, value) = part.split("=", limit = 2)
            key.uppercase() to value
        }

        val freqStr = parts["FREQ"] ?: return null
        val frequency = try {
            RecurrenceRule.Frequency.valueOf(freqStr)
        } catch (_: IllegalArgumentException) {
            return null
        }

        val interval = parts["INTERVAL"]?.toIntOrNull() ?: 1

        val daysOfWeek = parts["BYDAY"]?.split(",")
            ?.mapNotNull { ICAL_TO_DAY_OF_WEEK[it.uppercase()] }
            ?.toSet()
            ?: emptySet()

        val endDate = parts["UNTIL"]?.let { dateStr ->
            try {
                java.time.LocalDate.parse(dateStr, DATE_FORMATTER)
            } catch (_: Exception) {
                null
            }
        }

        return RecurrenceRule(
            frequency = frequency,
            daysOfWeek = daysOfWeek,
            interval = interval,
            startDate = startDate,
            endDate = endDate
        )
    }
}
