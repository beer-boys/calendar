package ru.itmo.dws.calendar.core.service.feed

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
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
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
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
    private val today = LocalDate.now(zoneId)

    private lateinit var habitRepository: InMemoryHabitRepository
    private lateinit var occurrenceRepository: InMemoryHabitOccurrenceRepository
    private lateinit var habitOccurrenceEventProvider: HabitOccurrenceEventProvider

    @BeforeEach
    fun setUp() {
        habitRepository = InMemoryHabitRepository()
        occurrenceRepository = InMemoryHabitOccurrenceRepository(habitRepository)
        habitOccurrenceEventProvider = HabitOccurrenceEventProvider(
            habitRepository = habitRepository,
            occurrenceRepository = occurrenceRepository,
            zoneId = zoneId
        )
    }

    @Nested
    @DisplayName("Получение ленты без внешнего провайдера")
    inner class WithoutExternalProvider {

        @Test
        @DisplayName("Возвращает пустой список если нет событий")
        fun `returns empty list when no events`() {
            val service = createService(calendarProvider = null)
            val timeRange = createTimeRange(today, today.plusDays(7))

            val result = service.getCalendarFeed(userId, timeRange)

            assertTrue(result.events.isEmpty())
            assertEquals(0, result.totalCount)
        }

        @Test
        @DisplayName("Возвращает occurrences привычек с правильным типом")
        fun `returns habit occurrences with correct type`() {
            val habit = createAndSaveHabit()
            val occurrence = createAndSaveOccurrence(habit, today)

            val service = createService(calendarProvider = null)
            val timeRange = createTimeRange(today, today.plusDays(1))

            val result = service.getCalendarFeed(userId, timeRange)

            assertEquals(1, result.events.size)
            val event = result.events.first()
            assertEquals(CalendarItemType.HABIT, event.itemType)
            assertEquals(EventSource.INTERNAL_ONLY, event.source)
            assertEquals(habit.title, event.title)

            val details = event.details as ItemDetails.Habit
            assertEquals(habit.id, details.habitId)
            assertEquals(today, details.occurrenceDate)
        }

        @Test
        @DisplayName("События сортируются по времени начала")
        fun `events are sorted by start time`() {
            val habit = createAndSaveHabit()
            createAndSaveOccurrence(habit, today.plusDays(1), LocalTime.of(14, 0))
            createAndSaveOccurrence(habit, today, LocalTime.of(9, 0))

            val service = createService(calendarProvider = null)
            val timeRange = createTimeRange(today, today.plusDays(2))

            val result = service.getCalendarFeed(userId, timeRange)

            assertEquals(2, result.events.size)
            assertTrue(result.events[0].startTime.isBefore(result.events[1].startTime))
        }
    }

    @Nested
    @DisplayName("Merge внутренних и внешних событий")
    inner class MergeEvents {

        @Test
        @DisplayName("Зеркалированные события не дублируются")
        fun `mirrored events are not duplicated`() {
            val habit = createAndSaveHabit()
            val externalEventId = "ext-event-123"
            val occurrence = createAndSaveOccurrence(habit, today, externalEventId = externalEventId)

            val mockProvider = MockCalendarProvider(
                listOf(
                    createExternalEvent(externalEventId, today, LocalTime.of(10, 0))
                )
            )

            val service = createService(calendarProvider = mockProvider)
            val timeRange = createTimeRange(today, today.plusDays(1))

            val result = service.getCalendarFeed(userId, timeRange)

            assertEquals(1, result.events.size)
            val event = result.events.first()
            assertEquals(CalendarItemType.HABIT, event.itemType)
            assertEquals(EventSource.MIRRORED, event.source)
            assertEquals(externalEventId, event.externalEventId)
        }

        @Test
        @DisplayName("Внешние события без связи с внутренними отображаются как EXTERNAL")
        fun `external events without internal link are shown as EXTERNAL`() {
            val externalEventId = "pure-external-event"
            val mockProvider = MockCalendarProvider(
                listOf(
                    createExternalEvent(externalEventId, today, LocalTime.of(10, 0))
                )
            )

            val service = createService(calendarProvider = mockProvider)
            val timeRange = createTimeRange(today, today.plusDays(1))

            val result = service.getCalendarFeed(userId, timeRange)

            assertEquals(1, result.events.size)
            val event = result.events.first()
            assertEquals(CalendarItemType.EXTERNAL, event.itemType)
            assertEquals(EventSource.EXTERNAL_ONLY, event.source)
            assertEquals(externalEventId, event.externalEventId)
        }
    }

    @Nested
    @DisplayName("Обнаружение конфликтов")
    inner class ConflictDetection {

        @Test
        @DisplayName("Пересекающиеся события помечаются как конфликтные")
        fun `overlapping events are marked as conflicting`() {
            val habit = createAndSaveHabit()
            val occurrence1 = createAndSaveOccurrence(habit, today, LocalTime.of(10, 0))
            val occurrence2 = createAndSaveOccurrence(habit, today.plusDays(1), LocalTime.of(10, 30))

            val mockProvider = MockCalendarProvider(
                listOf(
                    createExternalEvent("ext-1", today, LocalTime.of(10, 30), Duration.ofMinutes(30))
                )
            )

            val service = createService(calendarProvider = mockProvider)
            val timeRange = createTimeRange(today, today.plusDays(2))

            val result = service.getCalendarFeed(userId, timeRange)

            assertTrue(result.hasConflicts)
            val conflictingEvents = result.events.filter { it.conflict != null }
            assertTrue(conflictingEvents.isNotEmpty())
        }

        @Test
        @DisplayName("Непересекающиеся события не имеют конфликтов")
        fun `non-overlapping events have no conflicts`() {
            val habit = createAndSaveHabit()
            createAndSaveOccurrence(habit, today, LocalTime.of(9, 0))
            createAndSaveOccurrence(habit, today.plusDays(1), LocalTime.of(14, 0))

            val service = createService(calendarProvider = null)
            val timeRange = createTimeRange(today, today.plusDays(2))

            val result = service.getCalendarFeed(userId, timeRange)

            assertTrue(result.events.all { it.conflict == null })
        }
    }

    private fun createService(calendarProvider: CalendarProvider?): CalendarFeedService {
        return CalendarFeedService(
            calendarProvider = calendarProvider,
            eventProviders = listOf(habitOccurrenceEventProvider)
        )
    }

    private fun createTimeRange(start: LocalDate, end: LocalDate): TimeSlot {
        return TimeSlot(
            start = ZonedDateTime.of(start, LocalTime.MIN, zoneId),
            end = ZonedDateTime.of(end, LocalTime.MAX, zoneId)
        )
    }

    private fun createAndSaveHabit(): Habit {
        val habit = Habit(
            id = HabitId.generate(),
            userId = userId,
            title = "Test Habit",
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
        externalEventId: String? = null
    ): HabitOccurrence {
        val occurrence = HabitOccurrence(
            habitId = habit.id,
            date = date,
            status = OccurrenceStatus.SCHEDULED,
            timeSlot = TimeSlot(
                start = ZonedDateTime.of(date, startTime, zoneId),
                end = ZonedDateTime.of(date, startTime.plusHours(1), zoneId)
            ),
            externalEventId = externalEventId
        )
        occurrenceRepository.save(occurrence)
        return occurrence
    }

    private fun createExternalEvent(
        id: String,
        date: LocalDate,
        startTime: LocalTime,
        duration: Duration = Duration.ofHours(1)
    ): CalendarEvent {
        return CalendarEvent(
            externalId = id,
            calendarId = CalendarId("primary"),
            owner = userId,
            timeSlot = TimeSlot(
                start = ZonedDateTime.of(date, startTime, zoneId),
                end = ZonedDateTime.of(date, startTime.plus(duration), zoneId)
            ),
            title = "External Event",
            description = null,
            participants = emptyList()
        )
    }

    private class MockCalendarProvider(
        private val events: List<CalendarEvent>
    ) : CalendarProvider {
        override fun getEvents(userId: UserId, timeRange: TimeSlot): List<CalendarEvent> = events
        override fun getEventsForUsers(userIds: List<UserId>, timeRange: TimeSlot) =
            emptyMap<UserId, List<CalendarEvent>>()

        override fun createEvent(userId: UserId, event: SchedulableEvent) = ""
        override fun updateEvent(userId: UserId, externalEventId: String, event: SchedulableEvent) = false
        override fun deleteEvent(userId: UserId, externalEventId: String) = false
    }
}
