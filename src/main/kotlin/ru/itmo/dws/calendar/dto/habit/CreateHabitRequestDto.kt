package ru.itmo.dws.calendar.dto.habit

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
    val title: String,
    val description: String? = null,
    val durationMinutes: Long,
    val recurrence: RecurrenceDto,
    val flexibility: FlexibilityDto,
    val priority: Int? = null,
    val bufferBeforeMinutes: Long? = null,
    val bufferAfterMinutes: Long? = null,
    val preferredStartTime: LocalTime? = null,
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
    val frequency: String,
    val daysOfWeek: Set<DayOfWeek>? = null,
    val interval: Int = 1,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
) {
    fun toDomain(): RecurrenceRule {
        return RecurrenceRule(
            frequency = RecurrenceRule.Frequency.valueOf(frequency.uppercase()),
            daysOfWeek = daysOfWeek ?: emptySet(),
            interval = interval,
            startDate = startDate ?: LocalDate.now(),
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
    val earliestTime: LocalTime,
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
    val periodDays: Long,
    val minOccurrences: Int? = null,
    val maxOccurrences: Int? = null,
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
    val dayOfWeek: DayOfWeek,
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
    val start: LocalTime,
    val end: LocalTime
) {
    fun toDomain(): TimeRange {
        return TimeRange(start = start, end = end)
    }
}

data class TimeSlotOverrideRequestDto(
    val newStartTime: LocalTime? = null,
    val newEndTime: LocalTime? = null,
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
