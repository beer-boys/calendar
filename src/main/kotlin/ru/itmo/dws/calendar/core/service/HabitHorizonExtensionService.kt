package ru.itmo.dws.calendar.core.service

import java.time.LocalDate
import java.time.ZoneId
import org.slf4j.LoggerFactory
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.model.OccurrenceStatus
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.port.output.HabitOccurrenceRepository
import ru.itmo.dws.calendar.core.port.output.HabitRepository
import ru.itmo.dws.calendar.core.service.provider.SchedulableEventProvider

@Suppress("LongParameterList")
class HabitHorizonExtensionService(
    private val habitRepository: HabitRepository,
    private val occurrenceRepository: HabitOccurrenceRepository,
    private val habitSyncService: HabitSyncService,
    private val eventProviders: List<SchedulableEventProvider>,
    private val eventSlotFinder: EventSlotFinder,
    private val horizonWeeks: Int = DEFAULT_HORIZON_WEEKS,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    private val log = LoggerFactory.getLogger(HabitHorizonExtensionService::class.java)

    fun extendAllHorizons(): ExtensionResult {
        val today = LocalDate.now(zoneId)
        val targetHorizon = today.plusWeeks(horizonWeeks.toLong())

        val allHabits = habitRepository.findAllHabits()

        var extended = 0
        var failed = 0

        for (habit in allHabits) {
            try {
                if (extendHabitHorizon(habit, today, targetHorizon)) {
                    extended++
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                log.warn("Failed to extend horizon for habit {}: {}", habit.id, e.message)
                failed++
            }
        }

        log.info("Horizon extension completed: {} habits extended, {} failed", extended, failed)
        return ExtensionResult(extended, failed)
    }

    fun extendHabitHorizon(habit: Habit, today: LocalDate, targetHorizon: LocalDate): Boolean {
        val existingOccurrences = occurrenceRepository.findByHabitId(habit.id)
        val maxExistingDate = existingOccurrences.maxOfOrNull { it.date } ?: today.minusDays(1)

        if (maxExistingDate >= targetHorizon) {
            return false
        }

        val startDate = maxExistingDate.plusDays(1)
        val newOccurrences = generateOccurrences(habit, startDate, targetHorizon)

        if (newOccurrences.isEmpty()) {
            return false
        }

        val syncResult = habitSyncService.syncOccurrencesToExternalCalendar(habit, newOccurrences)

        log.info(
            "Extended horizon for habit {} by {} occurrences (from {} to {}), {} synced",
            habit.id,
            newOccurrences.size,
            startDate,
            targetHorizon,
            syncResult.syncedCount
        )

        return true
    }

    private fun generateOccurrences(
        habit: Habit,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitOccurrence> {
        return generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(endDate) }
            .filter { habit.shouldOccurOn(it) }
            .map { date -> planOccurrenceForDate(habit, date) }
            .toList()
    }

    private fun planOccurrenceForDate(habit: Habit, date: LocalDate): HabitOccurrence {
        val occupiedSlots = collectOccupiedSlotsForDate(habit, date)

        val slot = eventSlotFinder.findOptimalSlot(
            event = habit,
            date = date,
            baseTimeWindow = habit.flexibilityTimeRange(),
            eventDuration = habit.duration,
            occupiedSlots = occupiedSlots,
            bufferTime = habit.bufferTime,
            preferredStartTime = habit.preferredStartTime(),
            zoneId = zoneId
        )

        return if (slot != null) {
            HabitOccurrence(
                habitId = habit.id,
                date = date,
                status = OccurrenceStatus.SCHEDULED,
                timeSlot = slot
            )
        } else {
            HabitOccurrence(
                habitId = habit.id,
                date = date,
                status = OccurrenceStatus.UNSCHEDULED,
                reason = "No available slot in flexibility window"
            )
        }
    }

    private fun collectOccupiedSlotsForDate(habit: Habit, date: LocalDate): List<TimeSlot> {
        val allEvents = eventProviders.flatMap { provider ->
            provider.getEventsForUserOnDate(habit.userId, date)
        }
        return eventSlotFinder.collectOccupiedSlots(allEvents, habit.id.toString())
    }

    companion object {
        const val DEFAULT_HORIZON_WEEKS = 4
    }
}

data class ExtensionResult(
    val extendedCount: Int,
    val failedCount: Int
) {
    val totalProcessed: Int get() = extendedCount + failedCount
    val isFullySuccessful: Boolean get() = failedCount == 0
}
