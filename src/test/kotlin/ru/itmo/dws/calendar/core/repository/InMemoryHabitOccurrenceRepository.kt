package ru.itmo.dws.calendar.core.repository

import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.port.output.HabitOccurrenceRepository

open class InMemoryHabitOccurrenceRepository : HabitOccurrenceRepository {
    private val occurrences = ConcurrentHashMap<String, HabitOccurrence>()

    private fun key(habitId: HabitId, date: LocalDate): String = "${habitId.value}_$date"

    override fun save(occurrence: HabitOccurrence): HabitOccurrence {
        occurrences[key(occurrence.habitId, occurrence.date)] = occurrence
        return occurrence
    }

    override fun saveAll(occurrences: List<HabitOccurrence>): List<HabitOccurrence> {
        return occurrences.map { save(it) }
    }

    override fun findByHabitId(habitId: HabitId): List<HabitOccurrence> {
        return occurrences.values.filter { it.habitId == habitId }
    }

    override fun findByHabitIdAndDateRange(
        habitId: HabitId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitOccurrence> {
        return occurrences.values.filter {
            it.habitId == habitId &&
                !it.date.isBefore(startDate) &&
                !it.date.isAfter(endDate)
        }
    }

    override fun findByHabitIdAndDate(habitId: HabitId, date: LocalDate): HabitOccurrence? {
        return occurrences[key(habitId, date)]
    }

    override fun findByExternalEventId(externalEventId: String): HabitOccurrence? {
        return occurrences.values.find { it.externalEventId == externalEventId }
    }

    override fun update(occurrence: HabitOccurrence): Boolean {
        val key = key(occurrence.habitId, occurrence.date)
        if (occurrences.containsKey(key)) {
            occurrences[key] = occurrence
            return true
        }
        return false
    }

    override fun deleteByHabitId(habitId: HabitId): Int {
        val keysToRemove = occurrences.entries
            .filter { it.value.habitId == habitId }
            .map { it.key }

        keysToRemove.forEach { occurrences.remove(it) }
        return keysToRemove.size
    }

    override fun deleteByHabitIdAndDateRange(
        habitId: HabitId,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int {
        val keysToRemove = occurrences.entries
            .filter {
                it.value.habitId == habitId &&
                    !it.value.date.isBefore(startDate) &&
                    !it.value.date.isAfter(endDate)
            }
            .map { it.key }

        keysToRemove.forEach { occurrences.remove(it) }
        return keysToRemove.size
    }
}

