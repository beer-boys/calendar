package ru.itmo.dws.calendar.dto.habit

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.valueobject.DayTimeExclusion
import ru.itmo.dws.calendar.core.domain.valueobject.SchedulingRule
import ru.itmo.dws.calendar.core.domain.valueobject.TimeRange
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlotOverride

data class HabitResponseDto(
    val id: UUID,
    val title: String,
    val description: String?,
    val durationMinutes: Long,
    val recurrence: RecurrenceResponseDto,
    val flexibility: FlexibilityResponseDto,
    val priority: Int,
    val bufferBeforeMinutes: Long,
    val bufferAfterMinutes: Long,
    val currentTimeSlot: TimeSlotResponseDto?,
    val rules: List<SchedulingRuleResponseDto>
) {
    companion object {
        fun fromDomain(habit: Habit): HabitResponseDto {
            return HabitResponseDto(
                id = habit.id.value,
                title = habit.title,
                description = habit.description,
                durationMinutes = habit.duration.toMinutes(),
                recurrence = RecurrenceResponseDto(
                    frequency = habit.recurrenceRule.frequency.name,
                    daysOfWeek = habit.recurrenceRule.daysOfWeek,
                    interval = habit.recurrenceRule.interval,
                    endDate = habit.recurrenceRule.endDate
                ),
                flexibility = FlexibilityResponseDto(
                    earliestTime = habit.flexibilityWindow.earliestTime,
                    latestTime = habit.flexibilityWindow.latestTime,
                    allowCrossDayMove = habit.flexibilityWindow.allowCrossDayMove,
                    preferredDurationMinutes = habit.flexibilityWindow.preferredDuration?.toMinutes()
                ),
                priority = habit.priority.value,
                bufferBeforeMinutes = habit.bufferTime.before.toMinutes(),
                bufferAfterMinutes = habit.bufferTime.after.toMinutes(),
                currentTimeSlot = habit.currentTimeSlot?.let {
                    TimeSlotResponseDto(start = it.start, end = it.end)
                },
                rules = habit.schedulingRules.map { SchedulingRuleResponseDto.fromDomain(it) }
            )
        }
    }
}

data class RecurrenceResponseDto(
    val frequency: String,
    val daysOfWeek: Set<DayOfWeek>,
    val interval: Int,
    val endDate: LocalDate?
)

data class FlexibilityResponseDto(
    val earliestTime: LocalTime,
    val latestTime: LocalTime,
    val allowCrossDayMove: Boolean,
    val preferredDurationMinutes: Long?
)

data class TimeSlotResponseDto(
    val start: ZonedDateTime,
    val end: ZonedDateTime
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = TimeWindowRuleResponseDto::class, name = "time_window"),
    JsonSubTypes.Type(value = ExclusionRuleResponseDto::class, name = "exclusion"),
    JsonSubTypes.Type(value = FrequencyRuleResponseDto::class, name = "frequency"),
    JsonSubTypes.Type(value = RecurrenceExceptionRuleResponseDto::class, name = "recurrence_exception")
)
sealed interface SchedulingRuleResponseDto {
    companion object {
        fun fromDomain(rule: SchedulingRule): SchedulingRuleResponseDto {
            return when (rule) {
                is SchedulingRule.TimeWindowRule -> TimeWindowRuleResponseDto(
                    earliestTime = rule.earliestTime,
                    latestTime = rule.latestTime,
                    activeDateRangeStart = rule.activeDateRange?.start,
                    activeDateRangeEnd = rule.activeDateRange?.end,
                    activeDaysOfWeek = rule.activeDaysOfWeek
                )
                is SchedulingRule.ExclusionRule -> ExclusionRuleResponseDto(
                    excludedDates = rule.excludedDates.ifEmpty { null },
                    excludedDaysOfWeek = rule.excludedDaysOfWeek.ifEmpty { null },
                    excludedTimeRanges = rule.excludedTimeRanges.map {
                        DayTimeExclusionResponseDto.fromDomain(it)
                    }.ifEmpty { null },
                    excludeHolidays = rule.excludeHolidays
                )
                is SchedulingRule.FrequencyRule -> FrequencyRuleResponseDto(
                    periodDays = rule.period.toDays(),
                    minOccurrences = rule.minOccurrences,
                    maxOccurrences = rule.maxOccurrences,
                    minGapMinutes = rule.minGapBetweenOccurrences?.toMinutes()
                )
                is SchedulingRule.RecurrenceExceptionRule -> RecurrenceExceptionRuleResponseDto(
                    cancelledDates = rule.cancelledDates.ifEmpty { null },
                    modifiedOccurrences = rule.modifiedOccurrences.mapValues { (_, override) ->
                        TimeSlotOverrideResponseDto.fromDomain(override)
                    }.ifEmpty { null }
                )
            }
        }
    }
}

data class TimeWindowRuleResponseDto(
    val earliestTime: LocalTime,
    val latestTime: LocalTime,
    val activeDateRangeStart: LocalDate? = null,
    val activeDateRangeEnd: LocalDate? = null,
    val activeDaysOfWeek: Set<DayOfWeek>? = null
) : SchedulingRuleResponseDto

data class ExclusionRuleResponseDto(
    val excludedDates: Set<LocalDate>? = null,
    val excludedDaysOfWeek: Set<DayOfWeek>? = null,
    val excludedTimeRanges: List<DayTimeExclusionResponseDto>? = null,
    val excludeHolidays: Boolean = false
) : SchedulingRuleResponseDto

data class FrequencyRuleResponseDto(
    val periodDays: Long,
    val minOccurrences: Int? = null,
    val maxOccurrences: Int? = null,
    val minGapMinutes: Long? = null
) : SchedulingRuleResponseDto

data class RecurrenceExceptionRuleResponseDto(
    val cancelledDates: Set<LocalDate>? = null,
    val modifiedOccurrences: Map<LocalDate, TimeSlotOverrideResponseDto>? = null
) : SchedulingRuleResponseDto

data class DayTimeExclusionResponseDto(
    val dayOfWeek: DayOfWeek,
    val excludedRanges: List<TimeRangeResponseDto>
) {
    companion object {
        fun fromDomain(exclusion: DayTimeExclusion): DayTimeExclusionResponseDto {
            return DayTimeExclusionResponseDto(
                dayOfWeek = exclusion.dayOfWeek,
                excludedRanges = exclusion.excludedRanges.map { TimeRangeResponseDto.fromDomain(it) }
            )
        }
    }
}

data class TimeRangeResponseDto(
    val start: LocalTime,
    val end: LocalTime
) {
    companion object {
        fun fromDomain(range: TimeRange): TimeRangeResponseDto {
            return TimeRangeResponseDto(start = range.start, end = range.end)
        }
    }
}

data class TimeSlotOverrideResponseDto(
    val newStartTime: LocalTime?,
    val newEndTime: LocalTime?,
    val newDurationMinutes: Long?
) {
    companion object {
        fun fromDomain(override: TimeSlotOverride): TimeSlotOverrideResponseDto {
            return TimeSlotOverrideResponseDto(
                newStartTime = override.newStartTime,
                newEndTime = override.newEndTime,
                newDurationMinutes = override.newDuration?.toMinutes()
            )
        }
    }
}
