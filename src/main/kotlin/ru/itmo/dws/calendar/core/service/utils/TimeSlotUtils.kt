package ru.itmo.dws.calendar.core.service.utils

import java.time.Duration
import java.time.ZonedDateTime
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot

object TimeSlotUtils {

    fun clipToWindow(slot: TimeSlot, from: ZonedDateTime, to: ZonedDateTime): TimeSlot? {
        val start = maxOf(slot.start, from)
        val end = minOf(slot.end, to)
        return if (end.isAfter(start)) TimeSlot(start, end) else null
    }

    fun mergeOverlappingOrAdjacent(slots: List<TimeSlot>): List<TimeSlot> {
        if (slots.isEmpty()) return emptyList()

        val result = ArrayList<TimeSlot>(slots.size)
        var current = slots.first()

        for (i in 1 until slots.size) {
            val next = slots[i]
            val overlaps = current.overlapsWith(next)
            val adjacent = current.end == next.start
            current = if (overlaps || adjacent) {
                TimeSlot(
                    start = current.start,
                    end = maxOf(current.end, next.end)
                )
            } else {
                result.add(current)
                next
            }
        }
        result.add(current)
        return result
    }

    @Suppress("LoopWithTooManyJumpStatements")
    fun subtractBusyFromWindow(window: TimeSlot, busy: List<TimeSlot>): List<TimeSlot> {
        if (busy.isEmpty()) return listOf(window)

        val free = mutableListOf<TimeSlot>()
        var cursor = window.start

        for (b in busy) {
            if (b.end <= window.start) continue
            if (b.start >= window.end) break

            val bs = maxOf(b.start, window.start)
            val be = minOf(b.end, window.end)

            if (cursor.isBefore(bs)) {
                free += TimeSlot(cursor, bs)
            }
            cursor = maxOf(cursor, be)
            if (!cursor.isBefore(window.end)) break
        }

        if (cursor.isBefore(window.end)) free += TimeSlot(cursor, window.end)
        return free
    }

    fun sliceWindow(
        window: TimeSlot,
        duration: Duration,
        step: Duration,
        alignmentBase: ZonedDateTime
    ): List<TimeSlot> {
        if (!window.end.isAfter(window.start)) return emptyList()

        val res = mutableListOf<TimeSlot>()
        var start = alignToStep(window.start, step, alignmentBase)

        while (!start.plus(duration).isAfter(window.end)) {
            res += TimeSlot(start, start.plus(duration))
            start = start.plus(step)
        }
        return res
    }

    /**
     * Выравнивание старта слота к сетке step относительно alignmentBase.
     * Пример: base=00:00, step=15m, time=10:07 -> 10:15.
     *
     * Делаем расчёт в Instant, чтобы корректно переживать разные timezone/DST.
     */
    fun alignToStep(time: ZonedDateTime, step: Duration, alignmentBase: ZonedDateTime): ZonedDateTime {
        val baseInstant = alignmentBase.toInstant()
        val tInstant = time.toInstant()
        val diffSeconds = Duration.between(baseInstant, tInstant).seconds
        val stepSeconds = step.seconds

        if (diffSeconds <= 0) return time // уже не раньше base, и шаг не нужен (или время = base)

        val remainder = diffSeconds % stepSeconds
        if (remainder == 0L) return time

        val add = stepSeconds - remainder
        return time.plusSeconds(add)
    }
}
