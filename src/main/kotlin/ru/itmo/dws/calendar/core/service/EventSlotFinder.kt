package ru.itmo.dws.calendar.core.service

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import ru.itmo.dws.calendar.core.domain.model.ProposedSlot
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.model.SlotTradeoff
import ru.itmo.dws.calendar.core.domain.valueobject.BufferDuration
import ru.itmo.dws.calendar.core.domain.valueobject.SchedulingContext
import ru.itmo.dws.calendar.core.domain.valueobject.TimeRange
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot

class EventSlotFinder(
    private val slotInterval: Duration = Duration.ofMinutes(15),
    private val defaultZoneId: ZoneId = ZoneId.systemDefault(),
    private val scoringConfig: SlotScoringConfig = SlotScoringConfig.default()
) {
    fun findOptimalSlot(
        event: SchedulableEvent,
        date: LocalDate,
        baseTimeWindow: TimeRange,
        eventDuration: Duration,
        occupiedSlots: List<TimeSlot>,
        bufferTime: BufferDuration = BufferDuration.NONE,
        preferredStartTime: LocalTime? = null,
        zoneId: ZoneId = defaultZoneId
    ): TimeSlot? {
        val context = buildSchedulingContext(event, date, baseTimeWindow, eventDuration, zoneId)

        if (!context.isSchedulingAllowed()) return null

        val contextSlots = context.generateAvailableSlots(slotInterval)
        val availableSlots = filterAvailableSlots(contextSlots, occupiedSlots, bufferTime)

        if (availableSlots.isEmpty()) return null

        return if (preferredStartTime != null) {
            findClosestToPreferred(availableSlots, preferredStartTime)
        } else {
            availableSlots.first()
        }
    }

    fun generateProposedSlots(
        event: SchedulableEvent,
        date: LocalDate,
        baseTimeWindow: TimeRange,
        eventDuration: Duration,
        occupiedSlots: List<TimeSlot>,
        bufferTime: BufferDuration = BufferDuration.NONE,
        preferredStartTime: LocalTime? = null,
        maxSlots: Int = 5,
        zoneId: ZoneId = defaultZoneId
    ): List<ProposedSlot> {
        val context = buildSchedulingContext(event, date, baseTimeWindow, eventDuration, zoneId)

        if (!context.isSchedulingAllowed()) return emptyList()

        val contextSlots = context.generateAvailableSlots(slotInterval)
        val availableSlots = filterAvailableSlots(contextSlots, occupiedSlots, bufferTime)

        if (availableSlots.isEmpty()) return emptyList()

        return availableSlots
            .map { slot -> scoreSlot(slot, context.effectiveTimeWindow, preferredStartTime, occupiedSlots) }
            .sortedByDescending { it.score }
            .take(maxSlots)
    }

    fun buildSchedulingContext(
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

    fun collectOccupiedSlots(
        events: List<SchedulableEvent>,
        excludeEventId: String? = null
    ): List<TimeSlot> {
        return events
            .filter { excludeEventId == null || it.eventId != excludeEventId }
            .mapNotNull { it.effectiveTimeSlot() }
    }

    private fun filterAvailableSlots(
        possibleSlots: List<TimeSlot>,
        occupiedSlots: List<TimeSlot>,
        bufferTime: BufferDuration
    ): List<TimeSlot> {
        return possibleSlots.filter { slot ->
            val effectiveSlot = if (bufferTime.hasBuffer()) {
                slot.withBuffer(bufferTime)
            } else {
                slot
            }
            !occupiedSlots.any { occupied -> effectiveSlot.overlapsWith(occupied) }
        }
    }

    private fun findClosestToPreferred(slots: List<TimeSlot>, preferredTime: LocalTime): TimeSlot {
        return slots.minByOrNull { slot ->
            val slotTime = slot.start.toLocalTime()
            abs(
                Duration.between(
                    preferredTime.atDate(slot.start.toLocalDate()),
                    slotTime.atDate(slot.start.toLocalDate())
                ).toMinutes()
            )
        } ?: slots.first()
    }

    private fun scoreSlot(
        slot: TimeSlot,
        timeWindow: TimeRange,
        preferredStartTime: LocalTime?,
        occupiedSlots: List<TimeSlot>
    ): ProposedSlot {
        var score = scoringConfig.baseScore
        val tradeoffs = mutableListOf<SlotTradeoff>()
        val slotStartTime = slot.start.toLocalTime()

        score -= calculatePreferredTimeDeviation(slotStartTime, preferredStartTime, tradeoffs)
        score -= calculateWindowBoundaryPenalty(slotStartTime, timeWindow, slot, tradeoffs)
        score -= calculateBreakPenalty(slot, occupiedSlots, tradeoffs)

        return ProposedSlot(
            timeSlot = slot,
            score = maxOf(0, score),
            tradeoffs = tradeoffs.distinct()
        )
    }

    private fun calculatePreferredTimeDeviation(
        slotStartTime: LocalTime,
        preferredStartTime: LocalTime?,
        tradeoffs: MutableList<SlotTradeoff>
    ): Int {
        if (preferredStartTime == null) return 0

        val thresholds = scoringConfig.preferredTimeDeviationThresholds
        val diffMinutes = abs(Duration.between(preferredStartTime, slotStartTime).toMinutes())

        return when {
            diffMinutes > thresholds.majorDeviationMinutes -> {
                if (slotStartTime.isBefore(preferredStartTime)) {
                    tradeoffs.add(SlotTradeoff.EARLIER_THAN_USUAL)
                } else {
                    tradeoffs.add(SlotTradeoff.LATER_THAN_USUAL)
                }
                thresholds.majorDeviationPenalty
            }
            diffMinutes > thresholds.moderateDeviationMinutes -> thresholds.moderateDeviationPenalty
            diffMinutes > thresholds.minorDeviationMinutes -> thresholds.minorDeviationPenalty
            else -> 0
        }
    }

    private fun calculateWindowBoundaryPenalty(
        slotStartTime: LocalTime,
        timeWindow: TimeRange,
        slot: TimeSlot,
        tradeoffs: MutableList<SlotTradeoff>
    ): Int {
        var penalty = 0
        val windowDuration = Duration.between(timeWindow.start, timeWindow.end).toMinutes()
        val distanceFromStart = Duration.between(
            timeWindow.start.atDate(slot.start.toLocalDate()),
            slotStartTime.atDate(slot.start.toLocalDate())
        ).toMinutes()
        val distanceFromEnd = Duration.between(
            slotStartTime.atDate(slot.start.toLocalDate()),
            timeWindow.end.atDate(slot.start.toLocalDate())
        ).toMinutes()

        if (distanceFromStart < windowDuration * scoringConfig.windowBoundaryThreshold) {
            penalty += scoringConfig.windowBoundaryPenalty
            tradeoffs.add(SlotTradeoff.CLOSE_TO_WINDOW_START)
        }
        if (distanceFromEnd < windowDuration * scoringConfig.windowBoundaryThreshold) {
            penalty += scoringConfig.windowBoundaryPenalty
            tradeoffs.add(SlotTradeoff.CLOSE_TO_WINDOW_END)
        }

        return penalty
    }

    private fun calculateBreakPenalty(
        slot: TimeSlot,
        occupiedSlots: List<TimeSlot>,
        tradeoffs: MutableList<SlotTradeoff>
    ): Int {
        var penalty = 0
        val minBreak = scoringConfig.minimumBreakDuration

        occupiedSlots.forEach { occupied ->
            val gapBefore = Duration.between(occupied.end, slot.start)
            val gapAfter = Duration.between(slot.end, occupied.start)

            if (gapBefore.isPositive && gapBefore < minBreak) {
                penalty += scoringConfig.shortBreakPenalty
                tradeoffs.add(SlotTradeoff.SHORTER_BREAK_BEFORE)
            }
            if (gapAfter.isPositive && gapAfter < minBreak) {
                penalty += scoringConfig.shortBreakPenalty
                tradeoffs.add(SlotTradeoff.SHORTER_BREAK_AFTER)
            }
        }

        return penalty
    }
}
