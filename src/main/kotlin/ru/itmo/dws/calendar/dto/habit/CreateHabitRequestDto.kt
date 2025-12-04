package ru.itmo.dws.calendar.dto.habit

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import ru.itmo.dws.calendar.core.domain.model.CreateHabitRequest
import ru.itmo.dws.calendar.core.domain.valueobject.BufferDuration
import ru.itmo.dws.calendar.core.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.RecurrenceRule
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

data class CreateHabitRequestDto(
    val title: String,
    val description: String? = null,
    val durationMinutes: Long,
    val recurrence: RecurrenceDto,
    val flexibility: FlexibilityDto,
    val priority: Int? = null,
    val bufferBeforeMinutes: Long? = null,
    val bufferAfterMinutes: Long? = null,
    val preferredStartTime: LocalTime? = null
) {
    fun toDomain(userId: UserId): CreateHabitRequest {
        return CreateHabitRequest(
            userId = userId,
            title = title,
            description = description,
            duration = Duration.ofMinutes(durationMinutes),
            recurrenceRule = recurrence.toDomain(),
            flexibilityWindow = flexibility.toDomain(),
            priority = priority?.let { Priority(it) } ?: Priority.forHabit(),
            bufferTime = BufferDuration(
                before = Duration.ofMinutes(bufferBeforeMinutes ?: 0),
                after = Duration.ofMinutes(bufferAfterMinutes ?: 0)
            ),
            preferredStartTime = preferredStartTime
        )
    }
}

data class RecurrenceDto(
    val frequency: String,
    val daysOfWeek: Set<DayOfWeek>? = null,
    val interval: Int = 1,
    val endDate: LocalDate? = null
) {
    fun toDomain(): RecurrenceRule {
        return RecurrenceRule(
            frequency = RecurrenceRule.Frequency.valueOf(frequency.uppercase()),
            daysOfWeek = daysOfWeek ?: emptySet(),
            interval = interval,
            endDate = endDate
        )
    }
}

data class FlexibilityDto(
    val earliestTime: LocalTime,
    val latestTime: LocalTime,
    val allowCrossDayMove: Boolean = false,
    val preferredDurationMinutes: Long? = null
) {
    fun toDomain(): HabitFlexibilityWindow {
        return HabitFlexibilityWindow(
            earliestTime = earliestTime,
            latestTime = latestTime,
            allowCrossDayMove = allowCrossDayMove,
            preferredDuration = preferredDurationMinutes?.let { Duration.ofMinutes(it) }
        )
    }
}
