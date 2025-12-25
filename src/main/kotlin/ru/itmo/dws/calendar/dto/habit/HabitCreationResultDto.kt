package ru.itmo.dws.calendar.dto.habit

import ru.itmo.dws.calendar.core.port.input.HabitCreationResult

data class HabitCreationResultDto(
    val habit: HabitResponseDto,
    val scheduledSlot: TimeSlotResponseDto?,
    val conflicts: List<HabitConflictDto>,
    val status: String
) {
    companion object {
        fun fromDomain(result: HabitCreationResult): HabitCreationResultDto {
            return HabitCreationResultDto(
                habit = HabitResponseDto.fromDomain(result.habit),
                scheduledSlot = result.scheduledSlot?.let {
                    TimeSlotResponseDto(start = it.start, end = it.end)
                },
                conflicts = result.conflicts.map { HabitConflictDto.fromDomain(it) },
                status = result.status.name
            )
        }
    }
}

data class HabitConflictDto(
    val habitTitle: String,
    val conflictingEventTitle: String,
    val conflictType: String,
    val affectedDate: String
) {
    companion object {
        fun fromDomain(conflict: ru.itmo.dws.calendar.core.domain.model.HabitConflict): HabitConflictDto {
            return HabitConflictDto(
                habitTitle = conflict.habitTitle,
                conflictingEventTitle = conflict.conflictingEvent.title,
                conflictType = conflict.conflictType.name,
                affectedDate = conflict.affectedDate.toString()
            )
        }
    }
}
