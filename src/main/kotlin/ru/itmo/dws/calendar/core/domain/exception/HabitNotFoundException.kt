package ru.itmo.dws.calendar.core.domain.exception

import ru.itmo.dws.calendar.core.domain.valueobject.HabitId

class HabitNotFoundException(
    val habitId: HabitId
) : CalendarDomainException("Habit not found: ${habitId.value}")
