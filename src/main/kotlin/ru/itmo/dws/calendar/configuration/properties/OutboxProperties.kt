package ru.itmo.dws.calendar.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("outbox")
data class OutboxProperties(
    val batchSize: Int = 50,
    val maxRetries: Int = 5,
    val backoffBase: Double = 2.0,
    val stuckThreshold: Duration = Duration.ofMinutes(10),

    val pollInterval: Duration = Duration.ofSeconds(15),
    val rescueInterval: Duration = Duration.ofMinutes(5),
)
