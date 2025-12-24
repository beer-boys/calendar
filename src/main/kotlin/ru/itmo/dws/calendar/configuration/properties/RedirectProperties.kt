package ru.itmo.dws.calendar.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("redirect")
data class RedirectProperties(
    val success: String,
    val fail: String,
)
