package ru.itmo.dws.calendar.configuration

import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.itmo.dws.calendar.service.util.ClockService

@Configuration
class ClockConfiguration {

    @Bean
    fun clockService(): ClockService {
        return ClockService(Clock.systemUTC())
    }
}
