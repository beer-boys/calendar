package ru.itmo.dws.calendar.core.service

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.valueobject.SchedulingContext
import ru.itmo.dws.calendar.core.domain.valueobject.TimeRange
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot

class RuleEngine(
    private val defaultZoneId: ZoneId = ZoneId.systemDefault()
) {
    companion object {
        private const val MINUTES_PER_DAY = 24 * 60L
    }

    fun buildContext(
        event: SchedulableEvent,
        date: LocalDate,
        baseTimeWindow: TimeRange,
        eventDuration: Duration,
        zoneId: ZoneId = defaultZoneId
    ): SchedulingContext {
        var context = SchedulingContext.forDate(
            date = date,
            baseTimeWindow = baseTimeWindow,
            eventDuration = eventDuration,
            zoneId = zoneId
        )

        event.schedulingRules.forEach { rule ->
            context = rule.applyTo(context)
        }

        return context
    }

    fun isEventAllowedOnDate(
        event: SchedulableEvent,
        date: LocalDate,
        baseTimeWindow: TimeRange,
        eventDuration: Duration,
        zoneId: ZoneId = defaultZoneId
    ): Boolean {
        val context = buildContext(event, date, baseTimeWindow, eventDuration, zoneId)
        return context.isSchedulingAllowed()
    }

    fun isEventAllowedAt(
        event: SchedulableEvent,
        date: LocalDate,
        time: LocalTime,
        baseTimeWindow: TimeRange,
        eventDuration: Duration,
        zoneId: ZoneId = defaultZoneId
    ): Boolean {
        val context = buildContext(event, date, baseTimeWindow, eventDuration, zoneId)
        if (!context.isSchedulingAllowed()) return false
        if (!context.effectiveTimeWindow.contains(time)) return false
        return context.excludedTimeRanges.none { it.contains(time) }
    }

    fun getEffectiveTimeWindow(
        event: SchedulableEvent,
        date: LocalDate,
        baseTimeWindow: TimeRange,
        eventDuration: Duration,
        zoneId: ZoneId = defaultZoneId
    ): TimeRange? {
        val context = buildContext(event, date, baseTimeWindow, eventDuration, zoneId)

        if (!context.isSchedulingAllowed()) return null

        return context.effectiveTimeWindow
    }

    fun generateAvailableSlots(
        event: SchedulableEvent,
        date: LocalDate,
        baseTimeWindow: TimeRange,
        eventDuration: Duration,
        slotInterval: Duration = Duration.ofMinutes(15),
        zoneId: ZoneId = defaultZoneId
    ): List<TimeSlot> {
        val context = buildContext(event, date, baseTimeWindow, eventDuration, zoneId)
        return context.generateAvailableSlots(slotInterval)
    }

    fun filterAllowedSlots(
        event: SchedulableEvent,
        date: LocalDate,
        slots: List<TimeSlot>,
        baseTimeWindow: TimeRange,
        eventDuration: Duration,
        zoneId: ZoneId = defaultZoneId
    ): List<TimeSlot> {
        val context = buildContext(event, date, baseTimeWindow, eventDuration, zoneId)

        if (!context.isSchedulingAllowed()) return emptyList()

        return slots.filter { slot ->
            val startTime = slot.start.toLocalTime()
            val endTime = slot.end.toLocalTime()
            val crossesMidnight = slot.end.toLocalDate() != slot.start.toLocalDate()

            val withinWindow = context.effectiveTimeWindow.contains(startTime) &&
                (crossesMidnight || context.effectiveTimeWindow.contains(endTime))

            val notExcluded = if (crossesMidnight) {
                true
            } else {
                context.excludedTimeRanges.none { excluded ->
                    TimeRange(startTime, endTime).overlapsWith(excluded)
                }
            }

            withinWindow && notExcluded
        }
    }

    fun validateFrequency(
        event: SchedulableEvent,
        proposedDate: LocalDate,
        existingOccurrences: List<LocalDate>,
        baseTimeWindow: TimeRange,
        eventDuration: Duration,
        zoneId: ZoneId = defaultZoneId
    ): FrequencyValidation {
        val context = buildContext(event, date = proposedDate, baseTimeWindow, eventDuration, zoneId)

        val frequencyConstraint = context.frequencyConstraint
            ?: return FrequencyValidation.valid()

        val periodDays = maxOf(1L, frequencyConstraint.period.toDays())
        val periodStart = proposedDate.minusDays(periodDays - 1)

        val occurrencesInPeriod = existingOccurrences.count { date ->
            !date.isBefore(periodStart) && !date.isAfter(proposedDate)
        }

        if (frequencyConstraint.maxOccurrences != null) {
            if (occurrencesInPeriod >= frequencyConstraint.maxOccurrences) {
                return FrequencyValidation.tooManyOccurrences(
                    current = occurrencesInPeriod,
                    max = frequencyConstraint.maxOccurrences
                )
            }
        }

        if (frequencyConstraint.minGapBetweenOccurrences != null) {
            val minGapDays =
                (frequencyConstraint.minGapBetweenOccurrences.toMinutes() + MINUTES_PER_DAY - 1) / MINUTES_PER_DAY
            val tooClose = existingOccurrences.any { existing ->
                val daysBetween = kotlin.math.abs(proposedDate.toEpochDay() - existing.toEpochDay())
                daysBetween < minGapDays && existing != proposedDate
            }
            if (tooClose) {
                return FrequencyValidation.gapTooSmall(frequencyConstraint.minGapBetweenOccurrences)
            }
        }

        return FrequencyValidation.valid()
    }

    fun checkMinimumFrequency(
        event: SchedulableEvent,
        periodEndDate: LocalDate,
        existingOccurrences: List<LocalDate>,
        baseTimeWindow: TimeRange,
        eventDuration: Duration,
        zoneId: ZoneId = defaultZoneId
    ): MinimumFrequencyCheck {
        val context = buildContext(event, periodEndDate, baseTimeWindow, eventDuration, zoneId)

        val frequencyConstraint = context.frequencyConstraint
            ?: return MinimumFrequencyCheck.notApplicable()

        val minRequired = frequencyConstraint.minOccurrences
            ?: return MinimumFrequencyCheck.notApplicable()

        val periodDays = frequencyConstraint.period.toDays()
        val periodStart = periodEndDate.minusDays(periodDays - 1)

        val occurrencesInPeriod = existingOccurrences.count { date ->
            !date.isBefore(periodStart) && !date.isAfter(periodEndDate)
        }

        return if (occurrencesInPeriod >= minRequired) {
            MinimumFrequencyCheck.met(current = occurrencesInPeriod, required = minRequired)
        } else {
            MinimumFrequencyCheck.notMet(
                current = occurrencesInPeriod,
                required = minRequired,
                deficit = minRequired - occurrencesInPeriod
            )
        }
    }

    fun getRecurrenceException(
        event: SchedulableEvent,
        date: LocalDate,
        baseTimeWindow: TimeRange,
        eventDuration: Duration,
        zoneId: ZoneId = defaultZoneId
    ): RecurrenceExceptionResult {
        val context = buildContext(event, date, baseTimeWindow, eventDuration, zoneId)

        if (!context.isSchedulingAllowed() && context.cancellationReason != null) {
            return RecurrenceExceptionResult.Cancelled
        }

        val override = context.timeSlotOverride
        if (override != null) {
            return RecurrenceExceptionResult.Modified(override)
        }

        return RecurrenceExceptionResult.NoException
    }
}

sealed class FrequencyValidation {
    data object Valid : FrequencyValidation()

    data class TooManyOccurrences(
        val currentCount: Int,
        val maxAllowed: Int
    ) : FrequencyValidation()

    data class GapTooSmall(
        val requiredGap: Duration
    ) : FrequencyValidation()

    fun isValid(): Boolean = this is Valid

    companion object {
        fun valid(): FrequencyValidation = Valid
        fun tooManyOccurrences(current: Int, max: Int) = TooManyOccurrences(current, max)
        fun gapTooSmall(requiredGap: Duration) = GapTooSmall(requiredGap)
    }
}

sealed class MinimumFrequencyCheck {
    data object NotApplicable : MinimumFrequencyCheck()

    data class Met(
        val currentCount: Int,
        val requiredCount: Int
    ) : MinimumFrequencyCheck()

    data class NotMet(
        val currentCount: Int,
        val requiredCount: Int,
        val deficit: Int
    ) : MinimumFrequencyCheck()

    fun isMet(): Boolean = this !is NotMet

    companion object {
        fun notApplicable(): MinimumFrequencyCheck = NotApplicable
        fun met(current: Int, required: Int) = Met(current, required)
        fun notMet(current: Int, required: Int, deficit: Int) = NotMet(current, required, deficit)
    }
}

sealed class RecurrenceExceptionResult {
    data object NoException : RecurrenceExceptionResult()
    data object Cancelled : RecurrenceExceptionResult()
    data class Modified(
        val override: ru.itmo.dws.calendar.core.domain.valueobject.TimeSlotOverride
    ) : RecurrenceExceptionResult()
}
