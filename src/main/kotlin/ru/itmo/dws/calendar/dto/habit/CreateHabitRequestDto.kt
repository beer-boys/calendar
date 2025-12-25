package ru.itmo.dws.calendar.dto.habit

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import ru.itmo.dws.calendar.core.domain.model.CreateHabitRequest
import ru.itmo.dws.calendar.core.domain.valueobject.BufferDuration
import ru.itmo.dws.calendar.core.domain.valueobject.DateRange
import ru.itmo.dws.calendar.core.domain.valueobject.DayTimeExclusion
import ru.itmo.dws.calendar.core.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.RecurrenceRule
import ru.itmo.dws.calendar.core.domain.valueobject.SchedulingRule
import ru.itmo.dws.calendar.core.domain.valueobject.TimeRange
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlotOverride
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

data class CreateHabitRequestDto(
    @field:NotBlank(message = "Title cannot be blank")
    val title: String,

    val description: String? = null,

    @field:Positive(message = "Duration must be positive")
    val durationMinutes: Long,

    @field:NotNull(message = "Recurrence rule is required")
    @field:Valid
    val recurrence: RecurrenceDto,

    @field:NotNull(message = "Flexibility window is required")
    @field:Valid
    val flexibility: FlexibilityDto,

    @field:Min(value = 1, message = "Priority must be at least 1")
    val priority: Int? = null,

    @field:PositiveOrZero(message = "Buffer before minutes must be non-negative")
    val bufferBeforeMinutes: Long? = null,

    @field:PositiveOrZero(message = "Buffer after minutes must be non-negative")
    val bufferAfterMinutes: Long? = null,

    val preferredStartTime: LocalTime? = null,

    @field:Valid
    val rules: List<SchedulingRuleRequestDto>? = null
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
            preferredStartTime = preferredStartTime,
            rules = rules?.map { it.toDomain() } ?: emptyList()
        )
    }
}

data class RecurrenceDto(
    @field:NotBlank(message = "Frequency is required")
    val frequency: String,

    val daysOfWeek: Set<DayOfWeek>? = null,

    @field:Positive(message = "Interval must be positive")
    val interval: Int = 1,

    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
) {
    fun toDomain(): RecurrenceRule {
        val parsedFrequency = try {
            RecurrenceRule.Frequency.valueOf(frequency.uppercase())
        } catch (@Suppress("SwallowedException") e: IllegalArgumentException) {
            val validValues = RecurrenceRule.Frequency.entries.joinToString()
            error("Invalid frequency: '$frequency'. Must be one of: $validValues")
        }

        val effectiveStartDate = startDate ?: LocalDate.now()
        require(endDate == null || !endDate.isBefore(effectiveStartDate)) {
            "End date must be after start date"
        }

        return RecurrenceRule(
            frequency = parsedFrequency,
            daysOfWeek = daysOfWeek ?: emptySet(),
            interval = interval,
            startDate = effectiveStartDate,
            endDate = endDate
        )
    }
}

data class FlexibilityDto(
    @field:NotNull(message = "Earliest time is required")
    val earliestTime: LocalTime,

    @field:NotNull(message = "Latest time is required")
    val latestTime: LocalTime,

    val allowCrossDayMove: Boolean = false,

    @field:Positive(message = "Preferred duration must be positive")
    val preferredDurationMinutes: Long? = null
) {
    fun toDomain(): HabitFlexibilityWindow {
        require(latestTime.isAfter(earliestTime)) {
            "Latest time must be after earliest time"
        }

        return HabitFlexibilityWindow(
            earliestTime = earliestTime,
            latestTime = latestTime,
            allowCrossDayMove = allowCrossDayMove,
            preferredDuration = preferredDurationMinutes?.let { Duration.ofMinutes(it) }
        )
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = TimeWindowRuleRequestDto::class, name = "time_window"),
    JsonSubTypes.Type(value = ExclusionRuleRequestDto::class, name = "exclusion"),
    JsonSubTypes.Type(value = FrequencyRuleRequestDto::class, name = "frequency"),
    JsonSubTypes.Type(value = RecurrenceExceptionRuleRequestDto::class, name = "recurrence_exception")
)
sealed interface SchedulingRuleRequestDto {
    fun toDomain(): SchedulingRule
}

data class TimeWindowRuleRequestDto(
    @field:NotNull(message = "Earliest time is required")
    val earliestTime: LocalTime,

    @field:NotNull(message = "Latest time is required")
    val latestTime: LocalTime,

    val activeDateRangeStart: LocalDate? = null,
    val activeDateRangeEnd: LocalDate? = null,
    val activeDaysOfWeek: Set<DayOfWeek>? = null
) : SchedulingRuleRequestDto {
    override fun toDomain(): SchedulingRule = SchedulingRule.TimeWindowRule(
        earliestTime = earliestTime,
        latestTime = latestTime,
        activeDateRange = if (activeDateRangeStart != null || activeDateRangeEnd != null) {
            DateRange(activeDateRangeStart, activeDateRangeEnd)
        } else {
            null
        },
        activeDaysOfWeek = activeDaysOfWeek
    )
}

data class ExclusionRuleRequestDto(
    val excludedDates: Set<LocalDate>? = null,
    val excludedDaysOfWeek: Set<DayOfWeek>? = null,
    val excludedTimeRanges: List<DayTimeExclusionRequestDto>? = null,
    val excludeHolidays: Boolean = false
) : SchedulingRuleRequestDto {
    override fun toDomain(): SchedulingRule = SchedulingRule.ExclusionRule(
        excludedDates = excludedDates ?: emptySet(),
        excludedDaysOfWeek = excludedDaysOfWeek ?: emptySet(),
        excludedTimeRanges = excludedTimeRanges?.map { it.toDomain() } ?: emptyList(),
        excludeHolidays = excludeHolidays
    )
}

data class FrequencyRuleRequestDto(
    @field:Positive(message = "Period days must be positive")
    val periodDays: Long,

    @field:PositiveOrZero(message = "Min occurrences must be non-negative")
    val minOccurrences: Int? = null,

    @field:PositiveOrZero(message = "Max occurrences must be non-negative")
    val maxOccurrences: Int? = null,

    @field:PositiveOrZero(message = "Min gap minutes must be non-negative")
    val minGapMinutes: Long? = null
) : SchedulingRuleRequestDto {
    override fun toDomain(): SchedulingRule = SchedulingRule.FrequencyRule(
        period = Duration.ofDays(periodDays),
        minOccurrences = minOccurrences,
        maxOccurrences = maxOccurrences,
        minGapBetweenOccurrences = minGapMinutes?.let { Duration.ofMinutes(it) }
    )
}

data class RecurrenceExceptionRuleRequestDto(
    val cancelledDates: Set<LocalDate>? = null,
    val modifiedOccurrences: Map<LocalDate, TimeSlotOverrideRequestDto>? = null
) : SchedulingRuleRequestDto {
    override fun toDomain(): SchedulingRule = SchedulingRule.RecurrenceExceptionRule(
        cancelledDates = cancelledDates ?: emptySet(),
        modifiedOccurrences = modifiedOccurrences?.mapValues { (_, dto) -> dto.toDomain() } ?: emptyMap()
    )
}

data class DayTimeExclusionRequestDto(
    @field:NotNull(message = "Day of week is required")
    val dayOfWeek: DayOfWeek,

    @field:Valid
    val excludedRanges: List<TimeRangeRequestDto>? = null
) {
    fun toDomain(): DayTimeExclusion {
        return DayTimeExclusion(
            dayOfWeek = dayOfWeek,
            excludedRanges = excludedRanges?.map { it.toDomain() } ?: emptyList()
        )
    }
}

data class TimeRangeRequestDto(
    @field:NotNull(message = "Start time is required")
    val start: LocalTime,

    @field:NotNull(message = "End time is required")
    val end: LocalTime
) {
    fun toDomain(): TimeRange {
        return TimeRange(start = start, end = end)
    }
}

data class TimeSlotOverrideRequestDto(
    val newStartTime: LocalTime? = null,
    val newEndTime: LocalTime? = null,

    @field:Positive(message = "New duration must be positive")
    val newDurationMinutes: Long? = null
) {
    fun toDomain(): TimeSlotOverride {
        return TimeSlotOverride(
            newStartTime = newStartTime,
            newEndTime = newEndTime,
            newDuration = newDurationMinutes?.let { Duration.ofMinutes(it) }
        )
    }
}
