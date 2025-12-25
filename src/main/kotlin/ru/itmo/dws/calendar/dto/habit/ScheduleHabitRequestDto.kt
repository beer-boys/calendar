package ru.itmo.dws.calendar.dto.habit

import java.time.LocalDate
import java.time.ZonedDateTime

data class ScheduleHabitRequestDto(
    val date: LocalDate,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime
)
