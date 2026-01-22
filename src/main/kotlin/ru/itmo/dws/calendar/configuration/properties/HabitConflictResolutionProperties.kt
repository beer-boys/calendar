package ru.itmo.dws.calendar.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "habit.conflict-resolution")
data class HabitConflictResolutionProperties(
    val enabled: Boolean = true,
    val cron: String = "0 */15 * * * ?",
    val checkDaysAhead: Int = 7,
    val maxAlternativeDays: Int = 3
)
