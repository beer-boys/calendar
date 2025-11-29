package ru.itmo.dws.calendar.configuration

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import ru.itmo.dws.calendar.configuration.properties.OutboxProperties

@Configuration
@EnableConfigurationProperties(OutboxProperties::class)
class OutboxConfiguration
