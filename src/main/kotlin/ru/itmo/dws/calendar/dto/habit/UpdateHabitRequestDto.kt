package ru.itmo.dws.calendar.dto.habit

import java.time.Duration
import ru.itmo.dws.calendar.core.domain.model.UpdateHabitRequest
import ru.itmo.dws.calendar.core.domain.valueobject.BufferDuration
import ru.itmo.dws.calendar.core.domain.valueobject.Priority

data class UpdateHabitRequestDto(
    val title: String? = null,
    val description: String? = null,
    val durationMinutes: Long? = null,
    val recurrence: RecurrenceDto? = null,
    val flexibility: FlexibilityDto? = null,
    val priority: Int? = null,
    val bufferBeforeMinutes: Long? = null,
    val bufferAfterMinutes: Long? = null,
    val rules: List<SchedulingRuleRequestDto>? = null
) {
    fun toDomain(): UpdateHabitRequest {
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
