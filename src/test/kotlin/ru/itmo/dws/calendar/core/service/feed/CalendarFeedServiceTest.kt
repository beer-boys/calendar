package ru.itmo.dws.calendar.core.service.feed

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.itmo.dws.calendar.core.domain.model.CalendarEvent
import ru.itmo.dws.calendar.core.domain.model.CalendarItemType
import ru.itmo.dws.calendar.core.domain.model.EventSource
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.model.ItemDetails
import ru.itmo.dws.calendar.core.domain.model.OccurrenceStatus
import ru.itmo.dws.calendar.core.domain.valueobject.CalendarId
import ru.itmo.dws.calendar.core.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.RecurrenceRule
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.CalendarProvider
import ru.itmo.dws.calendar.core.repository.InMemoryHabitOccurrenceRepository
import ru.itmo.dws.calendar.core.repository.InMemoryHabitRepository

@DisplayName("CalendarFeedService - обогащённая лента событий календаря")
class CalendarFeedServiceTest {

    private val zoneId = ZoneId.of("Europe/Moscow")
    private val userId = UserId(UUID.randomUUID())
    private val today = LocalDate.now()

    private lateinit var habitRepository: InMemoryHabitRepository
    private lateinit var occurrenceRepository: InMemoryHabitOccurrenceRepository
    private lateinit var mockCalendarProvider: MockCalendarProvider
    private lateinit var calendarFeedService: CalendarFeedService

    @BeforeEach
    fun setUp() {
        habitRepository = InMemoryHabitRepository()
        occurrenceRepository = InMemoryHabitOccurrenceRepository(habitRepository)
        mockCalendarProvider = MockCalendarProvider()

        val habitOccurrenceEventProvider = HabitOccurrenceEventProvider(
            habitRepository = habitRepository,
            occurrenceRepository = occurrenceRepository,
            zoneId = zoneId
        )

        calendarFeedService = CalendarFeedService(
            calendarProvider = mockCalendarProvider,
            eventProviders = listOf(habitOccurrenceEventProvider)
        )
    }

    @Nested
    @DisplayName("Базовые сценарии")
    inner class BasicScenarios {

        @Test
        @DisplayName("Пустая лента без событий")
        fun `empty feed when no events`() {
            val timeRange = createTimeRange(today, today.plusDays(7))

            val result = calendarFeedService.getCalendarFeed(userId, timeRange)

            assertTrue(result.events.isEmpty())
            assertEquals(0, result.totalCount)
            assertFalse(result.hasConflicts)
            assertEquals(timeRange, result.period)
        }

        @Test
        @DisplayName("Привычки возвращаются с типом HABIT и корректными details")
        fun `habits are returned with HABIT type and correct details`() {
            val habit = createAndSaveHabit("Утренняя пробежка")
            val occurrence = createAndSaveOccurrence(habit, today)
            val timeRange = createTimeRange(today, today.plusDays(1))

            val result = calendarFeedService.getCalendarFeed(userId, timeRange)

            assertEquals(1, result.events.size)
            val feedItem = result.events.first()

            assertEquals(CalendarItemType.HABIT, feedItem.itemType)
            assertEquals(habit.title, feedItem.title)
            assertNotNull(feedItem.timeSlot)

            val details = feedItem.details as ItemDetails.Habit
            assertEquals(habit.id, details.habitId)
            assertEquals(today, details.occurrenceDate)
            assertEquals(OccurrenceStatus.SCHEDULED, details.occurrenceStatus)
        }

        @Test
        @DisplayName("События отсортированы по времени начала")
        fun `events are sorted by start time`() {
            val habit1 = createAndSaveHabit("Поздняя привычка")
            val habit2 = createAndSaveHabit("Ранняя привычка")

            createAndSaveOccurrence(habit1, today, LocalTime.of(14, 0))
            createAndSaveOccurrence(habit2, today, LocalTime.of(9, 0))

            val timeRange = createTimeRange(today, today.plusDays(1))

            val result = calendarFeedService.getCalendarFeed(userId, timeRange)

            assertEquals(2, result.events.size)
            // Ранняя привычка должна быть первой
            assertEquals("Ранняя привычка", result.events[0].title)
            assertEquals("Поздняя привычка", result.events[1].title)

            assertTrue(result.events[0].startTime.isBefore(result.events[1].startTime))
        }
    }

    @Nested
    @DisplayName("Merge событий")
    inner class MergeEvents {

        @Test
        @DisplayName("Зеркалированные события не дублируются - внешнее исключается")
        fun `mirrored events are not duplicated`() {
            val externalEventId = "google-event-123"

            val habit = createAndSaveHabit("Синхронизированная привычка")
            createAndSaveOccurrence(habit, today, externalEventId = externalEventId)

            // Добавляем такое же событие во внешний календарь
            mockCalendarProvider.addEvent(
                createExternalEvent(
                    externalId = externalEventId,
                    title = "Синхронизированная привычка",
                    startTime = LocalTime.of(10, 0)
                )
            )

            val timeRange = createTimeRange(today, today.plusDays(1))

            val result = calendarFeedService.getCalendarFeed(userId, timeRange)

            // Должно быть только одно событие - внутреннее (зеркалированное)
            assertEquals(1, result.events.size)
            assertEquals(CalendarItemType.HABIT, result.events[0].itemType)
            assertEquals(EventSource.MIRRORED, result.events[0].source)
            assertEquals(externalEventId, result.events[0].externalEventId)
        }

        @Test
        @DisplayName("Внешние события без связи показываются как EXTERNAL")
        fun `external events without link shown as EXTERNAL`() {
            mockCalendarProvider.addEvent(
                createExternalEvent(
                    externalId = "external-meeting-456",
                    title = "Внешняя встреча",
                    startTime = LocalTime.of(15, 0)
                )
            )

            val timeRange = createTimeRange(today, today.plusDays(1))

            val result = calendarFeedService.getCalendarFeed(userId, timeRange)

            assertEquals(1, result.events.size)
            val feedItem = result.events.first()

            assertEquals(CalendarItemType.EXTERNAL, feedItem.itemType)
            assertEquals(EventSource.EXTERNAL_ONLY, feedItem.source)
            assertEquals("Внешняя встреча", feedItem.title)

            // Capabilities для внешних событий - только чтение
            assertFalse(feedItem.capabilities.canDelete)
            assertFalse(feedItem.capabilities.canReschedule)
            assertFalse(feedItem.capabilities.canEdit)
        }
    }

    @Nested
    @DisplayName("Конфликты")
    inner class Conflicts {

        @Test
        @DisplayName("Пересекающиеся события помечаются как конфликтные")
        fun `overlapping events are marked as conflicting`() {
            val habit1 = createAndSaveHabit("Привычка 1")
            val habit2 = createAndSaveHabit("Привычка 2")

            // Создаём пересекающиеся события: 10:00-11:00 и 10:30-11:30
            createAndSaveOccurrence(habit1, today, LocalTime.of(10, 0), LocalTime.of(11, 0))
            createAndSaveOccurrence(habit2, today, LocalTime.of(10, 30), LocalTime.of(11, 30))

            val timeRange = createTimeRange(today, today.plusDays(1))

            val result = calendarFeedService.getCalendarFeed(userId, timeRange)

            assertEquals(2, result.events.size)
            assertTrue(result.hasConflicts)

            // Оба события должны иметь conflict info
            assertTrue(result.events.all { it.conflict != null })

            val conflict1 = result.events[0].conflict!!
            val conflict2 = result.events[1].conflict!!

            assertTrue(conflict1.conflictingEventIds.isNotEmpty())
            assertTrue(conflict2.conflictingEventIds.isNotEmpty())
        }

        @Test
        @DisplayName("Непересекающиеся события без конфликтов")
        fun `non-overlapping events have no conflicts`() {
            val habit1 = createAndSaveHabit("Утренняя привычка")
            val habit2 = createAndSaveHabit("Вечерняя привычка")

            // Не пересекаются: 9:00-10:00 и 18:00-19:00
            createAndSaveOccurrence(habit1, today, LocalTime.of(9, 0), LocalTime.of(10, 0))
            createAndSaveOccurrence(habit2, today, LocalTime.of(18, 0), LocalTime.of(19, 0))

            val timeRange = createTimeRange(today, today.plusDays(1))

            val result = calendarFeedService.getCalendarFeed(userId, timeRange)

            assertEquals(2, result.events.size)
            assertFalse(result.hasConflicts)

            // Оба события без конфликтов
            assertTrue(result.events.all { it.conflict == null })
        }
    }

    // ===== Helper methods =====

    private fun createAndSaveHabit(title: String): Habit {
        val habit = Habit(
            id = HabitId.generate(),
            userId = userId,
            title = title,
            duration = Duration.ofHours(1),
            recurrenceRule = RecurrenceRule.daily(today),
            flexibilityWindow = HabitFlexibilityWindow.workingHours()
        )
        habitRepository.saveHabit(habit)
        return habit
    }

    private fun createAndSaveOccurrence(
        habit: Habit,
        date: LocalDate,
        startTime: LocalTime = LocalTime.of(10, 0),
        endTime: LocalTime = startTime.plusHours(1),
        externalEventId: String? = null
    ): HabitOccurrence {
        val occurrence = HabitOccurrence(
            habitId = habit.id,
            date = date,
            status = OccurrenceStatus.SCHEDULED,
            timeSlot = TimeSlot(
                start = ZonedDateTime.of(date, startTime, zoneId),
                end = ZonedDateTime.of(date, endTime, zoneId)
            ),
            externalEventId = externalEventId
        )
        occurrenceRepository.save(occurrence)
        return occurrence
    }

    private fun createTimeRange(startDate: LocalDate, endDate: LocalDate): TimeSlot {
        return TimeSlot(
            start = ZonedDateTime.of(startDate, LocalTime.MIN, zoneId),
            end = ZonedDateTime.of(endDate, LocalTime.MAX, zoneId)
        )
    }

    private fun createExternalEvent(
        externalId: String,
        title: String,
        startTime: LocalTime
    ): CalendarEvent {
        return CalendarEvent(
            externalId = externalId,
            calendarId = CalendarId("primary"),
            owner = userId,
            timeSlot = TimeSlot(
                start = ZonedDateTime.of(today, startTime, zoneId),
                end = ZonedDateTime.of(today, startTime.plusHours(1), zoneId)
            ),
            title = title,
            description = null,
            participants = emptyList(),
            eventType = CalendarEvent.EventType.REGULAR,
            isAllDay = false
        )
    }

    /**
     * Mock CalendarProvider для тестирования
     */
    private class MockCalendarProvider : CalendarProvider {
        private val events = mutableListOf<CalendarEvent>()

        fun addEvent(event: CalendarEvent) {
            events.add(event)
        }

        override fun getEvents(userId: UserId, timeRange: TimeSlot): List<CalendarEvent> {
            return events.filter { it.owner == userId }
        }

        override fun getEventsForUsers(
            userIds: List<UserId>,
            timeRange: TimeSlot
        ): Map<UserId, List<CalendarEvent>> {
            return userIds.associateWith { userId -> events.filter { it.owner == userId } }
        }

        override fun createEvent(
            userId: UserId,
            event: ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
        ): String = "mock-event-id"

        override fun updateEvent(
            userId: UserId,
            externalEventId: String,
            event: ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
        ): Boolean = true

        override fun deleteEvent(userId: UserId, externalEventId: String): Boolean = true
    }
}
