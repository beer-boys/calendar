package ru.itmo.dws.calendar.configuration

import org.postgresql.util.PGobject
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import ru.itmo.dws.calendar.model.types.JsonbString

@Configuration
class JdbcConfiguration : AbstractJdbcConfiguration() {

    override fun jdbcCustomConversions(): JdbcCustomConversions {
        return JdbcCustomConversions(
            listOf(
                JsonbToStringConverter,
                JsonbStringToPgObject,
                PgObjectToJsonbString,
            )
        )
    }

    @ReadingConverter
    object JsonbToStringConverter : Converter<PGobject, String> {
        override fun convert(source: PGobject): String {
            return source.value ?: ""
        }
    }

    @ReadingConverter
    object PgObjectToJsonbString : Converter<PGobject, JsonbString> {
        override fun convert(source: PGobject): JsonbString =
            JsonbString(source.value ?: "{}")
    }

    @WritingConverter
    object JsonbStringToPgObject : Converter<JsonbString, PGobject> {
        override fun convert(source: JsonbString): PGobject =
            PGobject().apply {
                type = "jsonb"
                value = source.value
            }
    }
}
