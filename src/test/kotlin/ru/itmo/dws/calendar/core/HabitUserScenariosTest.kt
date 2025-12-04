package ru.itmo.dws.calendar.core

import java.time.DayOfWeek
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
import ru.itmo.dws.calendar.core.domain.model.EventType
import ru.itmo.dws.calendar.core.domain.model.FocusTime
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.Meeting
import ru.itmo.dws.calendar.core.domain.valueobject.FocusTimeId
import ru.itmo.dws.calendar.core.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.MeetingId
import ru.itmo.dws.calendar.core.domain.valueobject.RecurrenceRule
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.repository.InMemoryFocusTimeRepository
import ru.itmo.dws.calendar.core.repository.InMemoryHabitRepository
import ru.itmo.dws.calendar.core.repository.InMemoryMeetingRepository
import ru.itmo.dws.calendar.core.service.ConflictDetectionService
import ru.itmo.dws.calendar.core.service.EventSlotFinder
import ru.itmo.dws.calendar.core.service.provider.FocusTimeEventProvider
import ru.itmo.dws.calendar.core.service.provider.HabitEventProvider
import ru.itmo.dws.calendar.core.service.provider.MeetingEventProvider
import ru.itmo.dws.calendar.core.service.provider.SchedulableEventProvider

@DisplayName("Habit User Scenarios")
class HabitUserScenariosTest {

    private val zoneId = ZoneId.of("Europe/Moscow")
    private val today = LocalDate.now()
    private val userId = UserId(UUID.randomUUID())

    private lateinit var habitRepository: InMemoryHabitRepository
    private lateinit var meetingRepository: InMemoryMeetingRepository
    private lateinit var focusTimeRepository: InMemoryFocusTimeRepository

    private lateinit var eventProviders: List<SchedulableEventProvider>
    private lateinit var conflictDetectionService: ConflictDetectionService
    private lateinit var eventSlotFinder: EventSlotFinder

    @BeforeEach
    fun setUp() {
        habitRepository = InMemoryHabitRepository()
        meetingRepository = InMemoryMeetingRepository()
        focusTimeRepository = InMemoryFocusTimeRepository()

        eventProviders = listOf(
            MeetingEventProvider(meetingRepository, zoneId),
            HabitEventProvider(habitRepository, zoneId),
            FocusTimeEventProvider(focusTimeRepository, zoneId)
        )

        conflictDetectionService = ConflictDetectionService(eventProviders)
        eventSlotFinder = EventSlotFinder(defaultZoneId = zoneId)
    }

    @Nested
    @DisplayName("Сценарий: Создание привычки без конфликтов")
    inner class CreateHabitWithoutConflicts {

        @Test
        @DisplayName("Пользователь создаёт утреннюю привычку 'Кофе' - слот находится успешно")
        fun `user creates morning habit and gets optimal slot`() {
            val habit = createHabit(
                title = "Утренний кофе",
                duration = Duration.ofMinutes(30),
                flexibilityWindow = HabitFlexibilityWindow(
                    earliestTime = LocalTime.of(7, 0),
                    latestTime = LocalTime.of(9, 0)
                ),
                recurrenceRule = RecurrenceRule.daily()
            )

            val occupiedSlots = collectOccupiedSlots(userId, today)
            val optimalSlot = eventSlotFinder.findOptimalSlot(
                event = habit,
                date = today,
                baseTimeWindow = habit.flexibilityTimeRange(),
                eventDuration = habit.duration,
                occupiedSlots = occupiedSlots,
                zoneId = zoneId
            )

            assertNotNull(optimalSlot)
            assertEquals(LocalTime.of(7, 0), optimalSlot!!.start.toLocalTime())
        }
    }

    @Nested
    @DisplayName("Сценарий: Конфликт привычки со встречей")
    inner class HabitConflictsWithMeeting {

        @Test
        @DisplayName("Встреча занимает время привычки - система находит альтернативный слот")
        fun `meeting occupies habit time - system finds alternative slot`() {
            val meeting = createMeeting(
                title = "Утренний стендап",
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(9, 0)
            )
            meetingRepository.saveMeeting(meeting)

            val habit = createHabit(
                title = "Медитация",
                duration = Duration.ofMinutes(45),
                flexibilityWindow = HabitFlexibilityWindow(
                    earliestTime = LocalTime.of(7, 30),
                    latestTime = LocalTime.of(10, 0)
                ),
                recurrenceRule = RecurrenceRule.daily()
            )

            val occupiedSlots = collectOccupiedSlots(userId, today)
            val optimalSlot = eventSlotFinder.findOptimalSlot(
                event = habit,
                date = today,
                baseTimeWindow = habit.flexibilityTimeRange(),
                eventDuration = habit.duration,
                occupiedSlots = occupiedSlots,
                zoneId = zoneId
            )

            assertNotNull(optimalSlot)
            assertEquals(LocalTime.of(9, 0), optimalSlot!!.start.toLocalTime())
            assertEquals(LocalTime.of(9, 45), optimalSlot.end.toLocalTime())
        }

        @Test
        @DisplayName("Привычка запланирована, затем добавляется конфликтующая встреча - детектируется конфликт")
        fun `habit scheduled then conflicting meeting added - conflict detected`() {
            val habit = createHabit(
                title = "Тик-ток сессия",
                duration = Duration.ofHours(1),
                flexibilityWindow = HabitFlexibilityWindow(
                    earliestTime = LocalTime.of(12, 0),
                    latestTime = LocalTime.of(18, 0)
                ),
                recurrenceRule = RecurrenceRule.daily()
            ).copy(currentTimeSlot = createTimeSlot(LocalTime.of(14, 0), LocalTime.of(15, 0)))
            habitRepository.saveHabit(habit)

            val meeting = createMeeting(
                title = "Созвон со смежниками",
                startTime = LocalTime.of(14, 30),
                endTime = LocalTime.of(15, 30)
            )
            meetingRepository.saveMeeting(meeting)

            val conflicts = conflictDetectionService.detectAllConflictsForUser(userId, today)

            assertEquals(1, conflicts.size)
            val conflict = conflicts.first()
            val eventTypes = setOf(conflict.sourceEvent.eventType, conflict.conflictingEvent.eventType)
            assertTrue(eventTypes.contains(EventType.HABIT))
            assertTrue(eventTypes.contains(EventType.MEETING))
        }
    }

    @Nested
    @DisplayName("Сценарий: Несколько конфликтов в один день")
    inner class MultipleConflictsInOneDay {

        @Test
        @DisplayName("День заполнен встречами - привычка не может быть запланирована")
        fun `day fully booked with meetings - no slot available for habit`() {
            listOf(
                createMeeting("Встреча 1", LocalTime.of(9, 0), LocalTime.of(10, 30)),
                createMeeting("Встреча 2", LocalTime.of(10, 30), LocalTime.of(12, 0)),
                createMeeting("Встреча 3", LocalTime.of(12, 0), LocalTime.of(13, 30)),
                createMeeting("Встреча 4", LocalTime.of(14, 0), LocalTime.of(15, 30)),
                createMeeting("Встреча 5", LocalTime.of(15, 30), LocalTime.of(17, 0)),
                createMeeting("Встреча 6", LocalTime.of(17, 0), LocalTime.of(18, 0))
            ).forEach { meetingRepository.saveMeeting(it) }

            val habit = createHabit(
                title = "Спорт",
                duration = Duration.ofHours(1),
                flexibilityWindow = HabitFlexibilityWindow.workingHours(),
                recurrenceRule = RecurrenceRule.daily()
            )

            val occupiedSlots = collectOccupiedSlots(userId, today)
            val optimalSlot = eventSlotFinder.findOptimalSlot(
                event = habit,
                date = today,
                baseTimeWindow = habit.flexibilityTimeRange(),
                eventDuration = habit.duration,
                occupiedSlots = occupiedSlots,
                zoneId = zoneId
            )

            assertTrue(optimalSlot == null)
        }
    }

    @Nested
    @DisplayName("Сценарий: Поиск альтернативных слотов")
    inner class FindAlternativeSlots {

        @Test
        @DisplayName("Система предлагает несколько альтернативных слотов")
        fun `system suggests multiple alternative slots`() {
            val meeting = createMeeting(
                title = "Дейлик",
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0)
            )
            meetingRepository.saveMeeting(meeting)

            val habit = createHabit(
                title = "Йога",
                duration = Duration.ofMinutes(30),
                flexibilityWindow = HabitFlexibilityWindow(
                    earliestTime = LocalTime.of(7, 0),
                    latestTime = LocalTime.of(12, 0)
                ),
                recurrenceRule = RecurrenceRule.daily()
            )

            val occupiedSlots = collectOccupiedSlots(userId, today)
            val proposedSlots = eventSlotFinder.generateProposedSlots(
                event = habit,
                date = today,
                baseTimeWindow = habit.flexibilityTimeRange(),
                eventDuration = habit.duration,
                occupiedSlots = occupiedSlots,
                maxSlots = 5,
                zoneId = zoneId
            )

            assertTrue(proposedSlots.size >= 3)

            proposedSlots.forEach { proposed ->
                assertFalse(proposed.timeSlot.overlapsWith(meeting.timeSlot))
            }

            val scores = proposedSlots.map { it.score }
            assertEquals(scores.sortedDescending(), scores)
        }

        @Test
        @DisplayName("Предпочтительное время учитывается при ранжировании слотов")
        fun `preferred time is considered when ranking slots`() {
            val preferredTime = LocalTime.of(8, 0)

            val habit = createHabit(
                title = "Завтрак",
                duration = Duration.ofMinutes(30),
                flexibilityWindow = HabitFlexibilityWindow(
                    earliestTime = LocalTime.of(7, 0),
                    latestTime = LocalTime.of(10, 0)
                ),
                recurrenceRule = RecurrenceRule.daily()
            )

            val proposedSlots = eventSlotFinder.generateProposedSlots(
                event = habit,
                date = today,
                baseTimeWindow = habit.flexibilityTimeRange(),
                eventDuration = habit.duration,
                occupiedSlots = emptyList(),
                preferredStartTime = preferredTime,
                maxSlots = 20,
                zoneId = zoneId
            )

            val slotAt8 = proposedSlots.find { it.timeSlot.start.toLocalTime() == LocalTime.of(8, 0) }
            assertNotNull(slotAt8, "Должен быть слот на 8:00")

            val topSlots = proposedSlots.take(5)
            assertTrue(
                topSlots.any { it.timeSlot.start.toLocalTime() == LocalTime.of(8, 0) },
                "Слот на 8:00 должен быть в топ-5 предложений"
            )
        }
    }

    @Nested
    @DisplayName("Сценарий: Конфликт привычки с Focus Time")
    inner class HabitConflictsWithFocusTime {

        @Test
        @DisplayName("Привычка не может быть запланирована во время Focus Time")
        fun `habit cannot be scheduled during focus time`() {
            val focusTime = FocusTime(
                id = FocusTimeId.generate(),
                userId = userId,
                title = "Плотненько работаю",
                timeSlot = createTimeSlot(LocalTime.of(9, 0), LocalTime.of(12, 0))
            )
            focusTimeRepository.saveFocusTime(focusTime)

            val habit = createHabit(
                title = "Кофе-брейк",
                duration = Duration.ofMinutes(15),
                flexibilityWindow = HabitFlexibilityWindow(
                    earliestTime = LocalTime.of(9, 0),
                    latestTime = LocalTime.of(12, 0)
                ),
                recurrenceRule = RecurrenceRule.daily()
            )

            val occupiedSlots = collectOccupiedSlots(userId, today)
            val optimalSlot = eventSlotFinder.findOptimalSlot(
                event = habit,
                date = today,
                baseTimeWindow = habit.flexibilityTimeRange(),
                eventDuration = habit.duration,
                occupiedSlots = occupiedSlots,
                zoneId = zoneId
            )

            assertTrue(optimalSlot == null)
        }
    }

    @Nested
    @DisplayName("Сценарий: Еженедельные привычки")
    inner class WeeklyHabits {

        @Test
        @DisplayName("Привычка запланирована только на определённые дни недели")
        fun `habit scheduled only on specific days of week`() {
            val habit = createHabit(
                title = "Бег",
                duration = Duration.ofMinutes(45),
                flexibilityWindow = HabitFlexibilityWindow.morning(),
                recurrenceRule = RecurrenceRule.weekly(
                    setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
                )
            )

            val monday = today.with(DayOfWeek.MONDAY)
            val tuesday = today.with(DayOfWeek.TUESDAY)
            val wednesday = today.with(DayOfWeek.WEDNESDAY)

            assertTrue(habit.shouldOccurOn(monday))
            assertFalse(habit.shouldOccurOn(tuesday))
            assertTrue(habit.shouldOccurOn(wednesday))
        }
    }

    @Nested
    @DisplayName("Сценарий: Конфликт между двумя привычками")
    inner class HabitConflictsWithHabit {

        @Test
        @DisplayName("Две привычки в одно время - детектируется конфликт")
        fun `two habits at same time - conflict detected`() {
            val meditation = createHabit(
                title = "Медитация",
                duration = Duration.ofMinutes(30),
                flexibilityWindow = HabitFlexibilityWindow.morning(),
                recurrenceRule = RecurrenceRule.daily()
            ).copy(currentTimeSlot = createTimeSlot(LocalTime.of(7, 0), LocalTime.of(7, 30)))
            habitRepository.saveHabit(meditation)

            val exercise = createHabit(
                title = "Зарядка",
                duration = Duration.ofMinutes(30),
                flexibilityWindow = HabitFlexibilityWindow.morning(),
                recurrenceRule = RecurrenceRule.daily()
            ).copy(currentTimeSlot = createTimeSlot(LocalTime.of(7, 15), LocalTime.of(7, 45)))
            habitRepository.saveHabit(exercise)

            val conflicts = conflictDetectionService.detectAllConflictsForUser(userId, today)

            assertEquals(1, conflicts.size)
            val conflict = conflicts.first()
            assertEquals(EventType.HABIT, conflict.sourceEvent.eventType)
            assertEquals(EventType.HABIT, conflict.conflictingEvent.eventType)
        }
    }

    private fun createHabit(
        title: String,
        duration: Duration,
        flexibilityWindow: HabitFlexibilityWindow,
        recurrenceRule: RecurrenceRule
    ): Habit {
        return Habit(
            id = HabitId.generate(),
            userId = userId,
            title = title,
            duration = duration,
            recurrenceRule = recurrenceRule,
            flexibilityWindow = flexibilityWindow
        )
    }

    private fun createMeeting(
        title: String,
        startTime: LocalTime,
        endTime: LocalTime
    ): Meeting {
        return Meeting(
            id = MeetingId.generate(),
            creator = userId,
            timeSlot = createTimeSlot(startTime, endTime),
            participants = listOf(userId),
            title = title
        )
    }

    private fun createTimeSlot(startTime: LocalTime, endTime: LocalTime): TimeSlot {
        return TimeSlot(
            start = ZonedDateTime.of(today, startTime, zoneId),
            end = ZonedDateTime.of(today, endTime, zoneId)
        )
    }

    private fun collectOccupiedSlots(userId: UserId, date: LocalDate): List<TimeSlot> {
        val allEvents = eventProviders.flatMap { provider ->
            provider.getEventsForUserOnDate(userId, date)
        }
        return eventSlotFinder.collectOccupiedSlots(allEvents, null)
    }
}
