package ru.itmo.dws.calendar.core.domain.valueobject

import java.util.UUID

@JvmInline
value class FocusTimeId(val value: UUID) {
    companion object {
        fun generate(): FocusTimeId = FocusTimeId(UUID.randomUUID())
        fun of(value: String): FocusTimeId = FocusTimeId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}
