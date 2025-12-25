package ru.itmo.dws.calendar.dto.habit

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.time.Duration
import ru.itmo.dws.calendar.core.domain.model.UpdateHabitRequest
import ru.itmo.dws.calendar.core.domain.valueobject.BufferDuration
import ru.itmo.dws.calendar.core.domain.valueobject.Priority

data class UpdateHabitRequestDto(
    val title: String? = null,
    val description: String? = null,

    @field:Positive(message = "Duration must be positive")
    val durationMinutes: Long? = null,

    @field:Valid
    val recurrence: RecurrenceDto? = null,

    @field:Valid
    val flexibility: FlexibilityDto? = null,

    @field:Min(value = 1, message = "Priority must be at least 1")
    val priority: Int? = null,

    @field:PositiveOrZero(message = "Buffer before minutes must be non-negative")
    val bufferBeforeMinutes: Long? = null,

    @field:PositiveOrZero(message = "Buffer after minutes must be non-negative")
    val bufferAfterMinutes: Long? = null,

    @field:Valid
    val rules: List<SchedulingRuleRequestDto>? = null
) {
    fun toDomain(): UpdateHabitRequest {
        require(title == null || title.isNotBlank()) { "Title cannot be blank" }

        return UpdateHabitRequest(
            title = title,
            description = description,
            duration = durationMinutes?.let { Duration.ofMinutes(it) },
            recurrenceRule = recurrence?.toDomain(),
            flexibilityWindow = flexibility?.toDomain(),
            priority = priority?.let { Priority(it) },
            bufferTime = if (bufferBeforeMinutes != null || bufferAfterMinutes != null) {
                BufferDuration(
                    before = Duration.ofMinutes(bufferBeforeMinutes ?: 0),
                    after = Duration.ofMinutes(bufferAfterMinutes ?: 0)
                )
            } else {
                null
            },
            rules = rules?.map { it.toDomain() }
        )
    }
}
