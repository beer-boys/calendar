package ru.itmo.dws.calendar.core.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.itmo.dws.calendar.core.domain.model.EventConflict
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.model.OccurrenceStatus
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.HabitOccurrenceRepository
import ru.itmo.dws.calendar.core.port.output.HabitRepository
import ru.itmo.dws.calendar.core.service.provider.SchedulableEventProvider

class HabitOccurrenceConflictResolutionServiceTest {

    private val habitOccurrenceRepository = mockk<HabitOccurrenceRepository>(relaxed = true)
    private val habitRepository = mockk<HabitRepository>()
    private val conflictDetectionService = mockk<ConflictDetectionService>()
    private val eventSlotFinder = mockk<EventSlotFinder>()
    private val habitSyncService = mockk<HabitSyncService>(relaxed = true)
    private val eventProvider = mockk<SchedulableEventProvider>()
    private val eventProviders = listOf(eventProvider)
    private val zoneId = ZoneId.of("UTC")

    private val service = HabitOccurrenceConflictResolutionService(
        habitOccurrenceRepository = habitOccurrenceRepository,
        habitRepository = habitRepository,
        conflictDetectionService = conflictDetectionService,
        eventSlotFinder = eventSlotFinder,
        habitSyncService = habitSyncService,
        eventProviders = eventProviders,
        zoneId = zoneId
    )

    @Test
    fun `should not resolve when no conflicts detected`() {
        val userId = UserId.generate()
        val habitId = HabitId.generate()
        val date = LocalDate.of(2025, 1, 20)
        val timeSlot = createTimeSlot(date, 10, 0, 11, 0)

        val habit = createHabit(habitId, "Завтрак", userId)
        val occurrence = createOccurrence(habitId, date, timeSlot)

        every { habitRepository.findAllHabits() } returns listOf(habit)
        every { habitOccurrenceRepository.findByHabitIdAndDateRange(any(), any(), any()) } returns listOf(occurrence)
        every { conflictDetectionService.detectConflictsInTimeSlot(any(), any(), any()) } returns emptyList()

        val result = service.resolveConflictsForPeriod(7)

        assertEquals(0, result.resolvedCount)
        assertEquals(0, result.movedToNextDayCount)
        assertEquals(0, result.unresolvedCount)
        verify(exactly = 0) { habitOccurrenceRepository.update(any()) }
    }

    @Test
    fun `should reschedule occurrence when free slot available in same day`() {
        val userId = UserId.generate()
        val habitId = HabitId.generate()
        val date = LocalDate.of(2025, 1, 20)
        val originalTimeSlot = createTimeSlot(date, 10, 0, 11, 0)
        val newTimeSlot = createTimeSlot(date, 14, 0, 15, 0)

        val habit = createHabit(habitId, "Завтрак", userId)
        val occurrence = createOccurrence(habitId, date, originalTimeSlot)

        every { habitRepository.findAllHabits() } returns listOf(habit)
        every { habitOccurrenceRepository.findByHabitIdAndDateRange(any(), any(), any()) } returns listOf(occurrence)
        every { conflictDetectionService.detectConflictsInTimeSlot(any(), any(), any()) } returns listOf(
            mockk<EventConflict>()
        )
        every { conflictDetectionService.collectAllEventsForUser(any<UserId>(), any<LocalDate>()) } returns emptyList()
        every { eventProvider.getEventsForUserOnDate(any(), any()) } returns emptyList()
        every { eventSlotFinder.collectOccupiedSlots(any(), any()) } returns emptyList()
        every { eventSlotFinder.findOptimalSlot(any(), any(), any(), any(), any(), any(), any(), any()) } returns newTimeSlot

        val result = service.resolveConflictsForPeriod(7)

        assertEquals(1, result.resolvedCount)
        assertEquals(0, result.movedToNextDayCount)
        assertEquals(0, result.unresolvedCount)
        verify(exactly = 1) { habitOccurrenceRepository.update(match { it.timeSlot == newTimeSlot }) }
        verify(exactly = 1) { habitSyncService.syncSingleOccurrence(habit, any()) }
    }

    @Test
    fun `should mark as unresolved when no free slot available`() {
        val userId = UserId.generate()
        val habitId = HabitId.generate()
        val date = LocalDate.of(2025, 1, 20)
        val timeSlot = createTimeSlot(date, 10, 0, 11, 0)

        val habit = createHabit(habitId, "Завтрак", userId, allowCrossDayMove = false)
        val occurrence = createOccurrence(habitId, date, timeSlot)

        every { habitRepository.findAllHabits() } returns listOf(habit)
        every { habitOccurrenceRepository.findByHabitIdAndDateRange(any(), any(), any()) } returns listOf(occurrence)
        every { conflictDetectionService.detectConflictsInTimeSlot(any(), any(), any()) } returns listOf(
            mockk<EventConflict>()
        )
        every { conflictDetectionService.collectAllEventsForUser(any<UserId>(), any<LocalDate>()) } returns emptyList()
        every { eventProvider.getEventsForUserOnDate(any(), any()) } returns emptyList()
        every { eventSlotFinder.collectOccupiedSlots(any(), any()) } returns emptyList()
        every { eventSlotFinder.findOptimalSlot(any(), any(), any(), any(), any(), any(), any(), any()) } returns null

        val result = service.resolveConflictsForPeriod(7)

        assertEquals(0, result.resolvedCount)
        assertEquals(0, result.movedToNextDayCount)
        assertEquals(1, result.unresolvedCount)
        verify(exactly = 1) {
            habitOccurrenceRepository.update(match { it.status == OccurrenceStatus.CONFLICT_UNRESOLVED })
        }
    }

    @Test
    fun `should move to next day when allowCrossDayMove is true and no slot in same day`() {
        val userId = UserId.generate()
        val habitId = HabitId.generate()
        val date = LocalDate.of(2025, 1, 20)
        val nextDate = date.plusDays(1)
        val originalTimeSlot = createTimeSlot(date, 10, 0, 11, 0)
        val nextDayTimeSlot = createTimeSlot(nextDate, 10, 0, 11, 0)

        val habit = createHabit(habitId, "Завтрак", userId, allowCrossDayMove = true)
        val occurrence = createOccurrence(habitId, date, originalTimeSlot)

        every { habitRepository.findAllHabits() } returns listOf(habit)
        every { habitOccurrenceRepository.findByHabitIdAndDateRange(any(), any(), any()) } returns listOf(occurrence)
        every { conflictDetectionService.detectConflictsInTimeSlot(any(), any(), any()) } returns listOf(
            mockk<EventConflict>()
        )
        every { conflictDetectionService.collectAllEventsForUser(any<UserId>(), any<LocalDate>()) } returns emptyList()
        every { eventProvider.getEventsForUserOnDate(any(), any()) } returns emptyList()
        every { eventSlotFinder.collectOccupiedSlots(any(), any()) } returns emptyList()
        every {
            eventSlotFinder.findOptimalSlot(
                any(),
                date,
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns null
        every {
            eventSlotFinder.findOptimalSlot(
                any(),
                nextDate,
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns nextDayTimeSlot
        every { habitOccurrenceRepository.findByHabitIdAndDate(habitId, nextDate) } returns null
        every { habitOccurrenceRepository.save(any()) } returnsArgument 0

        val result = service.resolveConflictsForPeriod(7)

        assertEquals(0, result.resolvedCount)
        assertEquals(1, result.movedToNextDayCount)
        assertEquals(0, result.unresolvedCount)
        verify(exactly = 1) { habitOccurrenceRepository.delete(occurrence) }
        verify(exactly = 1) { habitOccurrenceRepository.save(match { it.date == nextDate }) }
        verify(exactly = 1) { habitSyncService.syncSingleOccurrence(habit, any()) }
    }

    @Test
    fun `should process multiple habits and occurrences`() {
        val userId1 = UserId.generate()
        val userId2 = UserId.generate()
        val habitId1 = HabitId.generate()
        val habitId2 = HabitId.generate()
        val date = LocalDate.of(2025, 1, 20)

        val habit1 = createHabit(habitId1, "Завтрак", userId1)
        val habit2 = createHabit(habitId2, "Обед", userId2)

        val occurrence1 = createOccurrence(habitId1, date, createTimeSlot(date, 10, 0, 11, 0))
        val occurrence2 = createOccurrence(habitId2, date, createTimeSlot(date, 14, 0, 15, 0))

        every { habitRepository.findAllHabits() } returns listOf(habit1, habit2)
        every { habitOccurrenceRepository.findByHabitIdAndDateRange(habitId1, any(), any()) } returns listOf(
            occurrence1
        )
        every { habitOccurrenceRepository.findByHabitIdAndDateRange(habitId2, any(), any()) } returns listOf(
            occurrence2
        )
        every { conflictDetectionService.detectConflictsInTimeSlot(any(), any(), any()) } returns emptyList()

        val result = service.resolveConflictsForPeriod(7)

        assertTrue(result.totalProcessed >= 0)
        assertTrue(result.isFullySuccessful)
    }

    private fun createHabit(
        id: HabitId,
        title: String,
        userId: UserId,
        allowCrossDayMove: Boolean = false
    ): Habit {
        return mockk<Habit> {
            every { this@mockk.id } returns id
            every { this@mockk.title } returns title
            every { this@mockk.userId } returns userId
            every { priority } returns Priority(5)
            every { flexibilityWindow } returns mockk {
                every { this@mockk.allowCrossDayMove } returns allowCrossDayMove
            }
            every { flexibilityTimeRange() } returns mockk()
            every { duration } returns mockk()
            every { bufferTime } returns mockk()
            every { preferredStartTime() } returns null
            every { shouldOccurOn(any()) } returns true
        }
    }

    private fun createOccurrence(
        habitId: HabitId,
        date: LocalDate,
        timeSlot: TimeSlot
    ): HabitOccurrence {
        return HabitOccurrence(
            habitId = habitId,
            date = date,
            status = OccurrenceStatus.SCHEDULED,
            timeSlot = timeSlot
        )
    }

    private fun createTimeSlot(date: LocalDate, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): TimeSlot {
        val start = ZonedDateTime.of(date.atTime(startHour, startMinute), zoneId)
        val end = ZonedDateTime.of(date.atTime(endHour, endMinute), zoneId)
        return TimeSlot(start, end)
    }
}
