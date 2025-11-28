package ru.itmo.dws.calendar.configuration

import com.google.api.client.json.gson.GsonFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GsonConfiguration {

    @Bean
    fun gsonFactory(): GsonFactory {
        return GsonFactory.getDefaultInstance()
    }
}
