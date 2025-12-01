package ru.itmo.dws.calendar.core.domain.valueobject

@JvmInline
value class CalendarId(val value: String) {
    init {
        require(value.isNotBlank()) { "CalendarId cannot be blank" }
    }

    override fun toString(): String = value
}
