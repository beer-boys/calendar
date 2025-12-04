package ru.itmo.dws.calendar.dto.habit

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID
import ru.itmo.dws.calendar.core.domain.model.Habit

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
    val currentTimeSlot: TimeSlotResponseDto?
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
                }
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
