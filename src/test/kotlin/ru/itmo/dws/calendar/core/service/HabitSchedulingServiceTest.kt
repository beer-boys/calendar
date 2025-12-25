package ru.itmo.dws.calendar.core.service

import java.time.DayOfWeek
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
import ru.itmo.dws.calendar.core.domain.model.Meeting
import ru.itmo.dws.calendar.core.domain.model.OccurrenceStatus
import ru.itmo.dws.calendar.core.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.MeetingId
import ru.itmo.dws.calendar.core.domain.valueobject.RecurrenceRule
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.repository.InMemoryFocusTimeRepository
import ru.itmo.dws.calendar.core.repository.InMemoryHabitRepository
import ru.itmo.dws.calendar.core.repository.InMemoryMeetingRepository
import ru.itmo.dws.calendar.core.service.provider.FocusTimeEventProvider
import ru.itmo.dws.calendar.core.service.provider.HabitEventProvider
import ru.itmo.dws.calendar.core.service.provider.MeetingEventProvider

@DisplayName("HabitSchedulingService - планирование экземпляров привычки")
class HabitSchedulingServiceTest {

    private val zoneId = ZoneId.of("Europe/Moscow")
    private val userId = UserId(UUID.randomUUID())
    private val today = LocalDate.now()

    private lateinit var habitRepository: InMemoryHabitRepository
    private lateinit var meetingRepository: InMemoryMeetingRepository
    private lateinit var focusTimeRepository: InMemoryFocusTimeRepository
    private lateinit var habitSchedulingService: HabitSchedulingService

    @BeforeEach
    fun setUp() {
        habitRepository = InMemoryHabitRepository()
        meetingRepository = InMemoryMeetingRepository()
        focusTimeRepository = InMemoryFocusTimeRepository()

        val eventProviders = listOf(
            MeetingEventProvider(meetingRepository, zoneId),
            HabitEventProvider(habitRepository, zoneId),
            FocusTimeEventProvider(focusTimeRepository, zoneId)
        )

        val eventSlotFinder = EventSlotFinder(defaultZoneId = zoneId)

        habitSchedulingService = HabitSchedulingService(
            eventProviders = eventProviders,
            eventSlotFinder = eventSlotFinder,
            calendarProvider = null,
            zoneId = zoneId
        )
    }

    @Nested
    @DisplayName("Планирование расписания на период")
    inner class PlanSchedule {

        @Test
        @DisplayName("Ежедневная привычка на 4 недели — все дни получают статус SCHEDULED")
        fun `daily habit for 4 weeks - all days are scheduled`() {
            val habit = createHabit(
                title = "Утренняя зарядка",
                duration = Duration.ofMinutes(30),
                recurrenceRule = RecurrenceRule.daily(startDate = today),
                flexibilityWindow = HabitFlexibilityWindow(
                    earliestTime = LocalTime.of(7, 0),
                    latestTime = LocalTime.of(9, 0)
                )
            )

            val plan = habitSchedulingService.planSchedule(habit, weeks = 4)

            assertEquals(habit.id, plan.habitId)
            assertEquals(habit.title, plan.habitTitle)
            assertEquals(today, plan.periodStart)
            assertEquals(today.plusWeeks(4), plan.periodEnd)

            // 4 недели + 1 день = 29 дней
            assertTrue(plan.occurrences.isNotEmpty())
            assertTrue(plan.occurrences.all { it.status == OccurrenceStatus.SCHEDULED })
            assertTrue(plan.occurrences.all { it.timeSlot != null })
        }

        @Test
        @DisplayName("Weekly привычка (Пн-Пт) — только будни включены в план")
        fun `weekly habit on weekdays - only weekdays are in plan`() {
            val workdays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )
            val habit = createHabit(
                title = "Обед",
                duration = Duration.ofHours(1),
                recurrenceRule = RecurrenceRule.weekly(startDate = today, daysOfWeek = workdays),
                flexibilityWindow = HabitFlexibilityWindow(
                    earliestTime = LocalTime.of(12, 0),
                    latestTime = LocalTime.of(14, 0)
                )
            )

            val plan = habitSchedulingService.planSchedule(habit, weeks = 2)

            // Проверяем, что все occurrence только на будни
            plan.occurrences.forEach { occurrence ->
                assertTrue(
                    workdays.contains(occurrence.date.dayOfWeek),
                    "Occurrence on ${occurrence.date} (${occurrence.date.dayOfWeek}) should be a weekday"
                )
            }

            // Проверяем, что выходных нет
            val weekendOccurrences = plan.occurrences.filter {
                it.date.dayOfWeek == DayOfWeek.SATURDAY || it.date.dayOfWeek == DayOfWeek.SUNDAY
            }
            assertTrue(weekendOccurrences.isEmpty(), "Weekends should not be in plan")
        }
    }

    @Nested
    @DisplayName("Учёт занятости при планировании")
    inner class OccupiedSlots {

        @Test
        @DisplayName("День полностью занят встречами — помечается как UNSCHEDULED с причиной")
        fun `day fully booked with meetings - marked as unscheduled`() {
            // Заполняем весь день встречами в окне гибкости
            val targetDate = today
            fillDayWithMeetings(
                date = targetDate,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(18, 0)
            )

            val habit = createHabit(
                title = "Спорт",
                duration = Duration.ofHours(1),
                recurrenceRule = RecurrenceRule.daily(startDate = today),
                flexibilityWindow = HabitFlexibilityWindow.workingHours()
            )

            val plan = habitSchedulingService.planSchedule(habit, weeks = 1)

            val todayOccurrence = plan.occurrences.find { it.date == targetDate }
            assertNotNull(todayOccurrence)
            assertEquals(OccurrenceStatus.UNSCHEDULED, todayOccurrence!!.status)
            assertNull(todayOccurrence.timeSlot)
            assertNotNull(todayOccurrence.reason)
            assertTrue(todayOccurrence.reason!!.isNotBlank())
        }
    }

    @Nested
    @DisplayName("Summary статистика")
    inner class Summary {

        @Test
        @DisplayName("Summary корректно считает scheduled/unscheduled/total")
        fun `summary counts are correct`() {
            // Один день занят полностью
            val busyDate = today.plusDays(1)
            fillDayWithMeetings(
                date = busyDate,
                startTime = LocalTime.of(6, 0),
                endTime = LocalTime.of(12, 0)
            )

            val habit = createHabit(
                title = "Утренняя медитация",
                duration = Duration.ofHours(1),
                recurrenceRule = RecurrenceRule.daily(startDate = today),
                flexibilityWindow = HabitFlexibilityWindow.morning()
            )

            val plan = habitSchedulingService.planSchedule(habit, weeks = 1)

            assertEquals(plan.occurrences.size, plan.totalCount)
            assertEquals(
                plan.occurrences.count { it.status == OccurrenceStatus.SCHEDULED },
                plan.scheduledCount
            )
            assertEquals(
                plan.occurrences.count { it.status == OccurrenceStatus.UNSCHEDULED },
                plan.unscheduledCount
            )

            // Как минимум один день должен быть UNSCHEDULED (busyDate)
            assertTrue(plan.unscheduledCount >= 1)
        }
    }

    private fun createHabit(
        title: String,
        duration: Duration,
        recurrenceRule: RecurrenceRule,
        flexibilityWindow: HabitFlexibilityWindow
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

    private fun fillDayWithMeetings(
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        slotDuration: Duration = Duration.ofHours(1)
    ) {
        var current = startTime
        while (current.plus(slotDuration) <= endTime) {
            val meeting = Meeting(
                id = MeetingId.generate(),
                creator = userId,
                title = "Meeting at $current",
                timeSlot = TimeSlot(
                    start = ZonedDateTime.of(date, current, zoneId),
                    end = ZonedDateTime.of(date, current.plus(slotDuration), zoneId)
                ),
                participants = listOf(userId)
            )
            meetingRepository.saveMeeting(meeting)
            current = current.plus(slotDuration)
        }
    }
}
