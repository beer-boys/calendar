package ru.itmo.dws.calendar.configuration

import com.google.api.client.json.gson.GsonFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class GsonConfiguration {

    @Bean
    open fun gsonFactory(): GsonFactory {
        return GsonFactory.getDefaultInstance()
    }
}
