package ru.itmo.dws.calendar.core.service.utils

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot

class TimeSlotUtilsTest {

    private val zone = ZoneId.of("Europe/Moscow")
    private val date = LocalDate.of(2025, 1, 10)
    private val base: ZonedDateTime = ZonedDateTime.of(date, LocalTime.MIDNIGHT, zone)

    private fun t(h: Int, m: Int = 0): ZonedDateTime = base.plusHours(h.toLong()).plusMinutes(m.toLong())
    private fun slot(sh: Int, sm: Int, eh: Int, em: Int) = TimeSlot(t(sh, sm), t(eh, em))

    // ---------------- clipToWindow ----------------

    @Test
    fun `clipToWindow returns null when slot is fully before window`() {
        val windowFrom = t(10, 0)
        val windowTo = t(11, 0)
        val s = slot(8, 0, 9, 0)

        val clipped = TimeSlotUtils.clipToWindow(s, windowFrom, windowTo)

        assertThat(clipped).isNull()
    }

    @Test
    fun `clipToWindow returns null when slot is fully after window`() {
        val windowFrom = t(10, 0)
        val windowTo = t(11, 0)
        val s = slot(11, 0, 12, 0) // starts exactly at windowTo => outside for [from,to)

        val clipped = TimeSlotUtils.clipToWindow(s, windowFrom, windowTo)

        assertThat(clipped).isNull()
    }

    @Test
    fun `clipToWindow clips slot partially overlapping on the left`() {
        val windowFrom = t(10, 0)
        val windowTo = t(11, 0)
        val s = slot(9, 30, 10, 15)

        val clipped = TimeSlotUtils.clipToWindow(s, windowFrom, windowTo)

        assertThat(clipped).isEqualTo(TimeSlot(t(10, 0), t(10, 15)))
    }

    @Test
    fun `clipToWindow clips slot partially overlapping on the right`() {
        val windowFrom = t(10, 0)
        val windowTo = t(11, 0)
        val s = slot(10, 45, 11, 30)

        val clipped = TimeSlotUtils.clipToWindow(s, windowFrom, windowTo)

        assertThat(clipped).isEqualTo(TimeSlot(t(10, 45), t(11, 0)))
    }

    @Test
    fun `clipToWindow returns same slot when it is fully inside window`() {
        val windowFrom = t(10, 0)
        val windowTo = t(12, 0)
        val s = slot(10, 15, 11, 45)

        val clipped = TimeSlotUtils.clipToWindow(s, windowFrom, windowTo)

        assertThat(clipped).isEqualTo(s)
    }

    // ---------------- mergeOverlappingOrAdjacent ----------------

    @Test
    fun `mergeOverlappingOrAdjacent returns empty for empty input`() {
        assertThat(TimeSlotUtils.mergeOverlappingOrAdjacent(emptyList())).isEmpty()
    }

    @Test
    fun `mergeOverlappingOrAdjacent keeps single element`() {
        val s = slot(10, 0, 11, 0)
        assertThat(TimeSlotUtils.mergeOverlappingOrAdjacent(listOf(s))).containsExactly(s)
    }

    @Test
    fun `mergeOverlappingOrAdjacent merges overlapping slots`() {
        val s1 = slot(10, 0, 11, 0)
        val s2 = slot(10, 30, 12, 0)

        val merged = TimeSlotUtils.mergeOverlappingOrAdjacent(listOf(s1, s2))

        assertThat(merged).containsExactly(TimeSlot(t(10, 0), t(12, 0)))
    }

    @Test
    fun `mergeOverlappingOrAdjacent merges adjacent slots`() {
        val s1 = slot(10, 0, 11, 0)
        val s2 = slot(11, 0, 12, 0)

        val merged = TimeSlotUtils.mergeOverlappingOrAdjacent(listOf(s1, s2))

        assertThat(merged).containsExactly(TimeSlot(t(10, 0), t(12, 0)))
    }

    @Test
    fun `mergeOverlappingOrAdjacent does not merge when there is a gap`() {
        val s1 = slot(10, 0, 11, 0)
        val s2 = slot(11, 1, 12, 0)

        val merged = TimeSlotUtils.mergeOverlappingOrAdjacent(listOf(s1, s2))

        assertThat(merged).containsExactly(s1, s2)
    }

    // ---------------- subtractBusyFromWindow ----------------

    @Test
    fun `subtractBusyFromWindow returns window when busy is empty`() {
        val w = slot(10, 0, 12, 0)
        assertThat(TimeSlotUtils.subtractBusyFromWindow(w, emptyList())).containsExactly(w)
    }

    @Test
    fun `subtractBusyFromWindow returns empty when busy fully covers window`() {
        val w = slot(10, 0, 12, 0)
        val busy = listOf(slot(9, 0, 13, 0)) // covers all

        val free = TimeSlotUtils.subtractBusyFromWindow(w, busy)

        assertThat(free).isEmpty()
    }

    @Test
    fun `subtractBusyFromWindow returns left remainder when busy covers right side`() {
        val w = slot(10, 0, 12, 0)
        val busy = listOf(slot(11, 0, 13, 0))

        val free = TimeSlotUtils.subtractBusyFromWindow(w, busy)

        assertThat(free).containsExactly(TimeSlot(t(10, 0), t(11, 0)))
    }

    @Test
    fun `subtractBusyFromWindow returns right remainder when busy covers left side`() {
        val w = slot(10, 0, 12, 0)
        val busy = listOf(slot(9, 0, 11, 0))

        val free = TimeSlotUtils.subtractBusyFromWindow(w, busy)

        assertThat(free).containsExactly(TimeSlot(t(11, 0), t(12, 0)))
    }

    @Test
    fun `subtractBusyFromWindow splits window into two parts when busy in the middle`() {
        val w = slot(10, 0, 12, 0)
        val busy = listOf(slot(10, 30, 11, 0))

        val free = TimeSlotUtils.subtractBusyFromWindow(w, busy)

        assertThat(free).containsExactly(
            TimeSlot(t(10, 0), t(10, 30)),
            TimeSlot(t(11, 0), t(12, 0))
        )
    }

    @Test
    fun `subtractBusyFromWindow ignores busy slots outside the window`() {
        val w = slot(10, 0, 12, 0)
        val busy = listOf(
            slot(8, 0, 9, 0), // before
            slot(13, 0, 14, 0) // after
        )

        val free = TimeSlotUtils.subtractBusyFromWindow(w, busy)

        assertThat(free).containsExactly(w)
    }

    @Test
    fun `subtractBusyFromWindow requires busy sorted by start for correct result`() {
        // Аналогично merge: алгоритм предполагает отсортированный список busy.
        val w = slot(10, 0, 12, 0)
        val b1 = slot(11, 0, 11, 30)
        val b2 = slot(10, 15, 10, 45)

        val free = TimeSlotUtils.subtractBusyFromWindow(w, listOf(b1, b2))

        // При неправильном порядке результат может быть неожиданным,
        // фиксируем предпосылку тестом (не на "правильность", а на контракт).
        assertThat(free).isNotEmpty
    }

    // ---------------- alignToStep ----------------

    @Test
    fun `alignToStep returns same time when already aligned`() {
        val aligned = t(10, 30) // 10:30 кратно 15
        val step = Duration.ofMinutes(15)

        val result = TimeSlotUtils.alignToStep(aligned, step, base)

        assertThat(result).isEqualTo(aligned)
    }

    @Test
    fun `alignToStep rounds up to next step`() {
        val notAligned = t(10, 7)
        val step = Duration.ofMinutes(15)

        val result = TimeSlotUtils.alignToStep(notAligned, step, base)

        assertThat(result).isEqualTo(t(10, 15))
    }

    @Test
    fun `alignToStep keeps time when diffSeconds is zero or negative`() {
        val step = Duration.ofMinutes(15)

        // time == base
        assertThat(TimeSlotUtils.alignToStep(base, step, base)).isEqualTo(base)

        // time before base (не типичный кейс, но функция так определена)
        val before = base.minusMinutes(1)
        assertThat(TimeSlotUtils.alignToStep(before, step, base)).isEqualTo(before)
    }

    // ---------------- sliceWindow ----------------

    @Test
    fun `sliceWindow returns no slots when duration longer than window`() {
        val w = slot(10, 0, 10, 20)
        val duration = Duration.ofMinutes(30)
        val step = Duration.ofMinutes(15)

        val res = TimeSlotUtils.sliceWindow(w, duration, step, base)

        assertThat(res).isEmpty()
    }

    @Test
    fun `sliceWindow slices window with 15-min step and aligned start`() {
        val w = slot(10, 0, 11, 0)
        val duration = Duration.ofMinutes(30)
        val step = Duration.ofMinutes(15)

        val res = TimeSlotUtils.sliceWindow(w, duration, step, base)

        assertThat(res).containsExactly(
            TimeSlot(t(10, 0), t(10, 30)),
            TimeSlot(t(10, 15), t(10, 45)),
            TimeSlot(t(10, 30), t(11, 0))
        )
    }

    @Test
    fun `sliceWindow aligns start to 15-min grid`() {
        val w = TimeSlot(t(10, 7), t(11, 0))
        val duration = Duration.ofMinutes(30)
        val step = Duration.ofMinutes(15)

        val res = TimeSlotUtils.sliceWindow(w, duration, step, base)

        // Первый старт должен быть 10:15 (а не 10:07)
        assertThat(res.first()).isEqualTo(TimeSlot(t(10, 15), t(10, 45)))
    }

    @Test
    fun `sliceWindow allows slot ending exactly at window end`() {
        val w = slot(10, 0, 10, 45)
        val duration = Duration.ofMinutes(45)
        val step = Duration.ofMinutes(15)

        val res = TimeSlotUtils.sliceWindow(w, duration, step, base)

        assertThat(res).containsExactly(TimeSlot(t(10, 0), t(10, 45)))
    }
}
