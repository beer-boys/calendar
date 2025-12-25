package ru.itmo.dws.calendar.dto.habit

import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.ZonedDateTime

data class ScheduleHabitRequestDto(
    @field:NotNull(message = "Date is required")
    val date: LocalDate,

    @field:NotNull(message = "Start time is required")
    val startTime: ZonedDateTime,

    @field:NotNull(message = "End time is required")
    val endTime: ZonedDateTime
)
