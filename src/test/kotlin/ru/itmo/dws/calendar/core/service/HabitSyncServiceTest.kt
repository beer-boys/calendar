package ru.itmo.dws.calendar.core.service

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.model.OccurrenceEvent
import ru.itmo.dws.calendar.core.domain.model.OccurrenceStatus
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.RecurrenceRule
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.CalendarProvider
import ru.itmo.dws.calendar.core.repository.InMemoryHabitOccurrenceRepository

@DisplayName("HabitSyncService - синхронизация экземпляров с внешним календарём")
class HabitSyncServiceTest {

    private val zoneId = ZoneId.of("Europe/Moscow")
    private val userId = UserId(UUID.randomUUID())
    private val today = LocalDate.now()

    private lateinit var occurrenceRepository: InMemoryHabitOccurrenceRepository

    @BeforeEach
    fun setUp() {
        occurrenceRepository = InMemoryHabitOccurrenceRepository()
    }

    @Nested
    @DisplayName("Синхронизация без CalendarProvider")
    inner class WithoutCalendarProvider {

        @Test
        @DisplayName("Без CalendarProvider экземпляры сохраняются локально, все попадают в skippedCount")
        fun `without calendar provider occurrences are saved locally only`() {
            val habitSyncService = HabitSyncService(
                occurrenceRepository = occurrenceRepository,
                calendarProvider = null
            )
            val habit = createHabit()
            val occurrences = listOf(
                createScheduledOccurrence(habit.id, today),
                createScheduledOccurrence(habit.id, today.plusDays(1)),
                createUnscheduledOccurrence(habit.id, today.plusDays(2))
            )

            val result = habitSyncService.syncOccurrencesToExternalCalendar(habit, occurrences)

            assertEquals(0, result.syncedCount)
            assertEquals(0, result.failedCount)
            assertEquals(3, result.skippedCount)
            assertEquals(3, result.occurrences.size)

            // Все экземпляры без externalEventId
            assertTrue(result.occurrences.all { it.externalEventId == null })

            // Проверяем, что сохранены в репозиторий
            val savedOccurrences = occurrenceRepository.findByHabitId(habit.id)
            assertEquals(3, savedOccurrences.size)
        }
    }

    @Nested
    @DisplayName("Синхронизация с CalendarProvider")
    inner class WithCalendarProvider {

        @Test
        @DisplayName("SCHEDULED экземпляры синхронизируются и получают externalEventId")
        fun `scheduled occurrences are synced and get external event id`() {
            val mockCalendarProvider = MockCalendarProvider()
            val habitSyncService = HabitSyncService(
                occurrenceRepository = occurrenceRepository,
                calendarProvider = mockCalendarProvider
            )
            val habit = createHabit()
            val occurrences = listOf(
                createScheduledOccurrence(habit.id, today),
                createScheduledOccurrence(habit.id, today.plusDays(1))
            )

            val result = habitSyncService.syncOccurrencesToExternalCalendar(habit, occurrences)

            assertEquals(2, result.syncedCount)
            assertEquals(0, result.failedCount)
            assertEquals(0, result.skippedCount)

            // Все синхронизированные экземпляры имеют externalEventId
            assertTrue(result.occurrences.all { it.externalEventId != null })
            assertTrue(result.occurrences.all { it.isSynced })

            // CalendarProvider был вызван для каждого экземпляра
            assertEquals(2, mockCalendarProvider.createEventCallCount)
        }

        @Test
        @DisplayName("UNSCHEDULED экземпляры не синхронизируются, попадают в skippedCount")
        fun `unscheduled occurrences are not synced and go to skipped count`() {
            val mockCalendarProvider = MockCalendarProvider()
            val habitSyncService = HabitSyncService(
                occurrenceRepository = occurrenceRepository,
                calendarProvider = mockCalendarProvider
            )
            val habit = createHabit()
            val occurrences = listOf(
                createScheduledOccurrence(habit.id, today),
                createUnscheduledOccurrence(habit.id, today.plusDays(1)),
                createUnscheduledOccurrence(habit.id, today.plusDays(2))
            )

            val result = habitSyncService.syncOccurrencesToExternalCalendar(habit, occurrences)

            assertEquals(1, result.syncedCount)
            assertEquals(0, result.failedCount)
            assertEquals(2, result.skippedCount)

            // UNSCHEDULED экземпляры без externalEventId
            val unscheduledResults = result.occurrences.filter { it.status == OccurrenceStatus.UNSCHEDULED }
            assertTrue(unscheduledResults.all { it.externalEventId == null })

            // CalendarProvider вызван только для SCHEDULED
            assertEquals(1, mockCalendarProvider.createEventCallCount)
        }

        @Test
        @DisplayName("SyncResult содержит корректную статистику (synced + failed + skipped = total)")
        fun `sync result contains correct statistics`() {
            val mockCalendarProvider = MockCalendarProvider()
            val habitSyncService = HabitSyncService(
                occurrenceRepository = occurrenceRepository,
                calendarProvider = mockCalendarProvider
            )
            val habit = createHabit()
            val occurrences = listOf(
                createScheduledOccurrence(habit.id, today),
                createScheduledOccurrence(habit.id, today.plusDays(1)),
                createUnscheduledOccurrence(habit.id, today.plusDays(2))
            )

            val result = habitSyncService.syncOccurrencesToExternalCalendar(habit, occurrences)

            assertEquals(3, result.totalCount)
            assertEquals(result.syncedCount + result.failedCount + result.skippedCount, result.totalCount)
            assertTrue(result.isFullySuccessful)
        }
    }

    @Nested
    @DisplayName("OccurrenceEvent - метаданные события")
    inner class OccurrenceEventMetadata {

        @Test
        @DisplayName("OccurrenceEvent содержит habitId, occurrenceDate и source в описании")
        fun `occurrence event contains metadata in description`() {
            val habit = createHabit()
            val occurrence = createScheduledOccurrence(habit.id, today)

            val occurrenceEvent = OccurrenceEvent(habit, occurrence)

            assertEquals(habit.title, occurrenceEvent.title)
            assertNotNull(occurrenceEvent.effectiveTimeSlot())
            assertEquals(habit.id.value.toString(), occurrenceEvent.habitId)
            assertEquals(today.toString(), occurrenceEvent.occurrenceDate)
            assertEquals("smart-calendar", occurrenceEvent.sourceApplication)

            // Описание содержит метаданные
            val description = occurrenceEvent.description!!
            assertTrue(description.contains("[SmartCalendar Metadata]"))
            assertTrue(description.contains("habitId: ${habit.id.value}"))
            assertTrue(description.contains("occurrenceDate: $today"))
            assertTrue(description.contains("source: smart-calendar"))
        }

        @Test
        @DisplayName("eventId формируется как habitId_date")
        fun `event id is formed as habit id underscore date`() {
            val habit = createHabit()
            val occurrence = createScheduledOccurrence(habit.id, today)

            val occurrenceEvent = OccurrenceEvent(habit, occurrence)

            assertEquals("${habit.id.value}_$today", occurrenceEvent.eventId)
        }
    }

    private fun createHabit(): Habit {
        return Habit(
            id = HabitId.generate(),
            userId = userId,
            title = "Тестовая привычка",
            duration = Duration.ofHours(1),
            recurrenceRule = RecurrenceRule.daily(today),
            flexibilityWindow = HabitFlexibilityWindow.workingHours()
        )
    }

    private fun createScheduledOccurrence(habitId: HabitId, date: LocalDate): HabitOccurrence {
        val startTime = LocalTime.of(10, 0)
        val endTime = LocalTime.of(11, 0)
        return HabitOccurrence(
            habitId = habitId,
            date = date,
            status = OccurrenceStatus.SCHEDULED,
            timeSlot = TimeSlot(
                start = ZonedDateTime.of(date, startTime, zoneId),
                end = ZonedDateTime.of(date, endTime, zoneId)
            )
        )
    }

    private fun createUnscheduledOccurrence(habitId: HabitId, date: LocalDate): HabitOccurrence {
        return HabitOccurrence(
            habitId = habitId,
            date = date,
            status = OccurrenceStatus.UNSCHEDULED,
            reason = "No available slot"
        )
    }

    /**
     * Mock CalendarProvider для тестирования
     */
    private class MockCalendarProvider : CalendarProvider {
        var createEventCallCount = 0
        var updateEventCallCount = 0
        var deleteEventCallCount = 0

        override fun createEvent(userId: UserId, event: SchedulableEvent): String {
            createEventCallCount++
            return "external-event-${UUID.randomUUID()}"
        }

        override fun updateEvent(userId: UserId, externalEventId: String, event: SchedulableEvent): Boolean {
            updateEventCallCount++
            return true
        }

        override fun deleteEvent(userId: UserId, externalEventId: String): Boolean {
            deleteEventCallCount++
            return true
        }

        override fun getEvents(
            userId: UserId,
            timeRange: TimeSlot
        ): List<ru.itmo.dws.calendar.core.domain.model.CalendarEvent> = emptyList()

        override fun getEventsForUsers(
            userIds: List<UserId>,
            timeRange: TimeSlot
        ): Map<UserId, List<ru.itmo.dws.calendar.core.domain.model.CalendarEvent>> = emptyMap()

        override fun createRecurringEvent(userId: UserId, habit: Habit): String {
            return "recurring-event-${UUID.randomUUID()}"
        }

        override fun updateRecurringEvent(userId: UserId, externalEventId: String, habit: Habit): Boolean {
            return true
        }
    }
}

