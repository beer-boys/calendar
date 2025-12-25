package ru.itmo.dws.calendar.core.domain.valueobject

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class SchedulingContext(
    val date: LocalDate,
    val zoneId: ZoneId,
    val baseTimeWindow: TimeRange,
    val eventDuration: Duration,
    val effectiveTimeWindow: TimeRange = baseTimeWindow,
    val excludedTimeRanges: List<TimeRange> = emptyList(),
    val isDateAllowed: Boolean = true,
    val cancellationReason: String? = null,
    val timeSlotOverride: TimeSlotOverride? = null,
    val frequencyConstraint: FrequencyConstraint? = null
) {
    fun isSchedulingAllowed(): Boolean = isDateAllowed && cancellationReason == null

    fun withNarrowedTimeWindow(newWindow: TimeRange): SchedulingContext {
        val intersectionStart = maxOf(effectiveTimeWindow.start, newWindow.start)
        val intersectionEnd = minOf(effectiveTimeWindow.end, newWindow.end)

        return if (intersectionStart < intersectionEnd) {
            copy(effectiveTimeWindow = TimeRange(intersectionStart, intersectionEnd))
        } else {
            copy(isDateAllowed = false, cancellationReason = "No valid time window after applying rules")
        }
    }

    fun withExcludedRanges(ranges: List<TimeRange>): SchedulingContext {
        return copy(excludedTimeRanges = excludedTimeRanges + ranges)
    }

    fun withDateExcluded(reason: String): SchedulingContext {
        return copy(isDateAllowed = false, cancellationReason = reason)
    }

    fun withTimeSlotOverride(override: TimeSlotOverride): SchedulingContext {
        return copy(timeSlotOverride = override)
    }

    fun withFrequencyConstraint(constraint: FrequencyConstraint): SchedulingContext {
        return copy(frequencyConstraint = constraint)
    }

    fun generateAvailableSlots(slotInterval: Duration = Duration.ofMinutes(15)): List<TimeSlot> {
        if (!isSchedulingAllowed()) return emptyList()

        val slots = mutableListOf<TimeSlot>()
        var currentStart = ZonedDateTime.of(date, effectiveTimeWindow.start, zoneId)
        val latestEnd = ZonedDateTime.of(date, effectiveTimeWindow.end, zoneId)

        while (true) {
            val slotEnd = currentStart.plus(eventDuration)
            if (slotEnd.isAfter(latestEnd)) break

            val candidateSlot = TimeSlot(currentStart, slotEnd)

            val crossesMidnight = slotEnd.toLocalDate() != currentStart.toLocalDate()
            val isExcluded = if (crossesMidnight) {
                false
            } else {
                val slotTimeRange = TimeRange(currentStart.toLocalTime(), slotEnd.toLocalTime())
                excludedTimeRanges.any { excluded -> slotTimeRange.overlapsWith(excluded) }
            }

            if (!isExcluded) {
                slots.add(candidateSlot)
            }

            currentStart = currentStart.plus(slotInterval)
        }

        return slots
    }

    fun getExcludedTimeSlots(): List<TimeSlot> {
        return excludedTimeRanges.map { range ->
            TimeSlot(
                start = ZonedDateTime.of(date, range.start, zoneId),
                end = ZonedDateTime.of(date, range.end, zoneId)
            )
        }
    }

    companion object {
        fun forDate(
            date: LocalDate,
            baseTimeWindow: TimeRange,
            eventDuration: Duration,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): SchedulingContext {
            return SchedulingContext(
                date = date,
                zoneId = zoneId,
                baseTimeWindow = baseTimeWindow,
                eventDuration = eventDuration
            )
        }
    }
}

data class FrequencyConstraint(
    val period: Duration,
    val minOccurrences: Int? = null,
    val maxOccurrences: Int? = null,
    val minGapBetweenOccurrences: Duration? = null
)
