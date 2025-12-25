package ru.itmo.dws.calendar.core.port.output

import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId

interface HabitOccurrenceRepository {

    fun save(occurrence: HabitOccurrence): HabitOccurrence

    fun saveAll(occurrences: List<HabitOccurrence>): List<HabitOccurrence>

    fun findByHabitId(habitId: HabitId): List<HabitOccurrence>

    fun findByHabitIdAndDateRange(
        habitId: HabitId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitOccurrence>

    fun findByHabitIdAndDate(habitId: HabitId, date: LocalDate): HabitOccurrence?

    fun findByExternalEventId(externalEventId: String): HabitOccurrence?

    fun update(occurrence: HabitOccurrence): Boolean

    fun deleteByHabitId(habitId: HabitId): Int

    fun deleteByHabitIdAndDateRange(
        habitId: HabitId,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int

    fun delete(occurrence: HabitOccurrence): Boolean
}
