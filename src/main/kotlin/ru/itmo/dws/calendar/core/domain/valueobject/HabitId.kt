package ru.itmo.dws.calendar.core.domain.valueobject

import java.util.UUID

@JvmInline
value class HabitId(val value: UUID) {
    companion object {
        fun generate(): HabitId = HabitId(UUID.randomUUID())
        fun of(value: String): HabitId = HabitId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}
