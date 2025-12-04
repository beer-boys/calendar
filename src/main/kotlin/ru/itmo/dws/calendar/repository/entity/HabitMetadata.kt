package ru.itmo.dws.calendar.repository.entity

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class HabitMetadata(
    val durationMinutes: Long,
    val recurrenceFrequency: String,
    val recurrenceDaysOfWeek: Set<DayOfWeek>?,
    val recurrenceInterval: Int,
    val recurrenceEndDate: LocalDate?,
    val flexibilityEarliestTime: LocalTime,
    val flexibilityLatestTime: LocalTime,
    val flexibilityAllowCrossDay: Boolean,
    val flexibilityPreferredDurationMinutes: Long?,
    val bufferBeforeMinutes: Long,
    val bufferAfterMinutes: Long,
    val currentTimeSlotStart: String?,
    val currentTimeSlotEnd: String?,
    val rules: List<SchedulingRuleDto>? = null
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = TimeWindowRuleDto::class, name = "time_window"),
    JsonSubTypes.Type(value = ExclusionRuleDto::class, name = "exclusion"),
    JsonSubTypes.Type(value = FrequencyRuleDto::class, name = "frequency"),
    JsonSubTypes.Type(value = RecurrenceExceptionRuleDto::class, name = "recurrence_exception")
)
sealed interface SchedulingRuleDto

data class TimeWindowRuleDto(
    val earliestTime: LocalTime,
    val latestTime: LocalTime,
    val activeDateRangeStart: LocalDate? = null,
    val activeDateRangeEnd: LocalDate? = null,
    val activeDaysOfWeek: Set<DayOfWeek>? = null
) : SchedulingRuleDto

data class ExclusionRuleDto(
    val excludedDates: Set<LocalDate>? = null,
    val excludedDaysOfWeek: Set<DayOfWeek>? = null,
    val excludedTimeRanges: List<DayTimeExclusionDto>? = null,
    val excludeHolidays: Boolean = false
) : SchedulingRuleDto

data class FrequencyRuleDto(
    val periodDays: Long,
    val minOccurrences: Int? = null,
    val maxOccurrences: Int? = null,
    val minGapMinutes: Long? = null
) : SchedulingRuleDto

data class RecurrenceExceptionRuleDto(
    val cancelledDates: Set<LocalDate>? = null,
    val modifiedOccurrences: Map<String, TimeSlotOverrideDto>? = null
) : SchedulingRuleDto

data class DayTimeExclusionDto(
    val dayOfWeek: DayOfWeek,
    val excludedRanges: List<TimeRangeDto>? = null
)

data class TimeRangeDto(
    val start: LocalTime,
    val end: LocalTime
)

data class TimeSlotOverrideDto(
    val newStartTime: LocalTime? = null,
    val newEndTime: LocalTime? = null,
    val newDurationMinutes: Long? = null
)
