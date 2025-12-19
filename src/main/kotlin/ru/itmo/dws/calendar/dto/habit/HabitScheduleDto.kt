package ru.itmo.dws.calendar.dto.habit

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.model.HabitSchedulePlan

data class HabitSchedulePlanDto(
    val habitId: UUID,
    val habitTitle: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val occurrences: List<HabitOccurrenceDto>,
    val summary: ScheduleSummaryDto
) {
    companion object {
        fun fromDomain(plan: HabitSchedulePlan): HabitSchedulePlanDto {
            return HabitSchedulePlanDto(
                habitId = plan.habitId.value,
                habitTitle = plan.habitTitle,
                periodStart = plan.periodStart,
                periodEnd = plan.periodEnd,
                occurrences = plan.occurrences.map { HabitOccurrenceDto.fromDomain(it) },
                summary = ScheduleSummaryDto(
                    totalDays = plan.totalCount,
                    scheduledDays = plan.scheduledCount,
                    unscheduledDays = plan.unscheduledCount
                )
            )
        }
    }
}

data class HabitOccurrenceDto(
    val date: LocalDate,
    val status: String,
    val startTime: ZonedDateTime?,
    val endTime: ZonedDateTime?,
    val reason: String?
) {
    companion object {
        fun fromDomain(occurrence: HabitOccurrence): HabitOccurrenceDto {
            return HabitOccurrenceDto(
                date = occurrence.date,
                status = occurrence.status.name,
                startTime = occurrence.timeSlot?.start,
                endTime = occurrence.timeSlot?.end,
                reason = occurrence.reason
            )
        }
    }
}

data class ScheduleSummaryDto(
    val totalDays: Int,
    val scheduledDays: Int,
    val unscheduledDays: Int
)
