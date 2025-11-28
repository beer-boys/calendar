package ru.itmo.dws.calendar.configuration

import org.postgresql.util.PGobject
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration

@Configuration
class JdbcConfiguration : AbstractJdbcConfiguration() {

    override fun jdbcCustomConversions(): JdbcCustomConversions {
        return JdbcCustomConversions(
            listOf(
                JsonbToStringConverter()
            )
        )
    }

    @ReadingConverter
    class JsonbToStringConverter : Converter<PGobject, String> {
        override fun convert(source: PGobject): String {
            return source.value ?: ""
        }
    }
}
