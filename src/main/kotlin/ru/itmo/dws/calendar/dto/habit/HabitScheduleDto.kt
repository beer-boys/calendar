package ru.itmo.dws.calendar.dto.habit

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.model.HabitSchedulePlan
import ru.itmo.dws.calendar.core.port.input.HabitSyncResult

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
    val reason: String?,
    val externalEventId: String?,
    val isSynced: Boolean
) {
    companion object {
        fun fromDomain(occurrence: HabitOccurrence): HabitOccurrenceDto {
            return HabitOccurrenceDto(
                date = occurrence.date,
                status = occurrence.status.name,
                startTime = occurrence.timeSlot?.start,
                endTime = occurrence.timeSlot?.end,
                reason = occurrence.reason,
                externalEventId = occurrence.externalEventId,
                isSynced = occurrence.isSynced
            )
        }
    }
}

data class ScheduleSummaryDto(
    val totalDays: Int,
    val scheduledDays: Int,
    val unscheduledDays: Int
)

data class HabitSyncResultDto(
    val habitId: UUID,
    val syncedCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val totalCount: Int,
    val isFullySuccessful: Boolean,
    val occurrences: List<HabitOccurrenceDto>
) {
    companion object {
        fun fromDomain(result: HabitSyncResult): HabitSyncResultDto {
            return HabitSyncResultDto(
                habitId = result.habitId.value,
                syncedCount = result.syncedCount,
                failedCount = result.failedCount,
                skippedCount = result.skippedCount,
                totalCount = result.totalCount,
                isFullySuccessful = result.isFullySuccessful,
                occurrences = result.occurrences.map { HabitOccurrenceDto.fromDomain(it) }
            )
        }
    }
}
