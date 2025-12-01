package ru.itmo.dws.calendar.core.domain.model

import java.time.Duration
import java.time.LocalTime
import ru.itmo.dws.calendar.core.domain.valueobject.BufferDuration
import ru.itmo.dws.calendar.core.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.RecurrenceRule
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

data class CreateHabitRequest(
    val userId: UserId,
    val title: String,
    val description: String? = null,
    val duration: Duration,
    val recurrenceRule: RecurrenceRule,
    val flexibilityWindow: HabitFlexibilityWindow,
    val priority: Priority = Priority.forHabit(),
    val bufferTime: BufferDuration = BufferDuration.NONE,
    val preferredStartTime: LocalTime? = null
) {
    init {
        require(title.isNotBlank()) { "Habit title cannot be blank" }
        require(!duration.isNegative && !duration.isZero) { "Duration must be positive" }
    }

    fun toHabit(id: HabitId = HabitId.generate()): Habit {
        return Habit(
            id = id,
            userId = userId,
            title = title,
            description = description,
            duration = duration,
            recurrenceRule = recurrenceRule,
            flexibilityWindow = flexibilityWindow,
            priority = priority,
            bufferTime = bufferTime
        )
    }
}

data class UpdateHabitRequest(
    val title: String? = null,
    val description: String? = null,
    val duration: Duration? = null,
    val recurrenceRule: RecurrenceRule? = null,
    val flexibilityWindow: HabitFlexibilityWindow? = null,
    val priority: Priority? = null,
    val bufferTime: BufferDuration? = null
) {
    fun applyTo(habit: Habit): Habit {
        return habit.copy(
            title = title ?: habit.title,
            description = description ?: habit.description,
            duration = duration ?: habit.duration,
            recurrenceRule = recurrenceRule ?: habit.recurrenceRule,
            flexibilityWindow = flexibilityWindow ?: habit.flexibilityWindow,
            priority = priority ?: habit.priority,
            bufferTime = bufferTime ?: habit.bufferTime
        )
    }
}
