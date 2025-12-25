package ru.itmo.dws.calendar.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("habit.horizon")
data class HabitHorizonProperties(
    val planningWeeks: Int = 4,
    val extensionCron: String = "0 0 2 * * ?",
    val extensionEnabled: Boolean = true
)
