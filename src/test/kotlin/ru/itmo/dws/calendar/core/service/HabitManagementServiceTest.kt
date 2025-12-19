package ru.itmo.dws.calendar.core.service

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.itmo.dws.calendar.core.domain.model.CreateHabitRequest
import ru.itmo.dws.calendar.core.domain.model.UpdateHabitRequest
import ru.itmo.dws.calendar.core.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.RecurrenceRule
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.input.HabitCreationStatus
import ru.itmo.dws.calendar.core.repository.InMemoryFocusTimeRepository
import ru.itmo.dws.calendar.core.repository.InMemoryHabitRepository
import ru.itmo.dws.calendar.core.repository.InMemoryMeetingRepository
import ru.itmo.dws.calendar.core.service.provider.FocusTimeEventProvider
import ru.itmo.dws.calendar.core.service.provider.HabitEventProvider
import ru.itmo.dws.calendar.core.service.provider.MeetingEventProvider

@DisplayName("HabitManagementService - CRUD операции с привычками")
class HabitManagementServiceTest {

    private val zoneId = ZoneId.of("Europe/Moscow")
    private val userId = UserId(UUID.randomUUID())
    private val today = LocalDate.now()

    private lateinit var habitRepository: InMemoryHabitRepository
    private lateinit var habitManagementService: HabitManagementService

    @BeforeEach
    fun setUp() {
        habitRepository = InMemoryHabitRepository()
        val meetingRepository = InMemoryMeetingRepository()
        val focusTimeRepository = InMemoryFocusTimeRepository()

        val eventProviders = listOf(
            MeetingEventProvider(meetingRepository, zoneId),
            HabitEventProvider(habitRepository, zoneId),
            FocusTimeEventProvider(focusTimeRepository, zoneId)
        )

        val conflictDetectionService = ConflictDetectionService(eventProviders)
        val eventSlotFinder = EventSlotFinder(defaultZoneId = zoneId)

        habitManagementService = HabitManagementService(
            habitRepository = habitRepository,
            eventProviders = eventProviders,
            eventSlotFinder = eventSlotFinder,
            conflictDetectionService = conflictDetectionService,
            calendarProvider = null,
            zoneId = zoneId
        )
    }

    @Nested
    @DisplayName("CREATE: Создание привычки")
    inner class CreateHabit {

        @Test
        @DisplayName("Пользователь создаёт привычку 'Обед' по будням 1 час в интервале 12:00-14:00 - привычка сохраняется")
        fun `user creates lunch habit on weekdays - habit is saved`() {
            val request = CreateHabitRequest(
                userId = userId,
                title = "Обед",
                description = "Перерыв на обед",
                duration = Duration.ofHours(1),
                recurrenceRule = RecurrenceRule.weekly(
                    startDate = today,
                    daysOfWeek = setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    )
                ),
                flexibilityWindow = HabitFlexibilityWindow(
                    earliestTime = LocalTime.of(12, 0),
                    latestTime = LocalTime.of(14, 0)
                )
            )

            val result = habitManagementService.createHabit(request)

            assertNotNull(result.habit)
            assertEquals("Обед", result.habit.title)
            assertEquals(Duration.ofHours(1), result.habit.duration)
            assertEquals(userId, result.habit.userId)

            val savedHabit = habitRepository.findHabit(result.habit.id)
            assertNotNull(savedHabit)
            assertEquals("Обед", savedHabit!!.title)
        }

        @Test
        @DisplayName("После создания привычки возвращается результат со статусом")
        fun `habit creation returns result with status`() {
            val request = createHabitRequest(title = "Спортзал")

            val result = habitManagementService.createHabit(request)

            assertNotNull(result.habit)
            assertTrue(
                result.status in listOf(
                    HabitCreationStatus.CREATED_WITH_SLOT,
                    HabitCreationStatus.CREATED_WITHOUT_SLOT
                )
            )
        }
    }

    @Nested
    @DisplayName("READ: Получение привычек")
    inner class ReadHabit {

        @Test
        @DisplayName("Получение привычки по ID и списка привычек пользователя")
        fun `get habit by id and list of user habits`() {
            val request1 = createHabitRequest(title = "Утренняя зарядка")
            val request2 = createHabitRequest(title = "Вечерняя прогулка")

            val result1 = habitManagementService.createHabit(request1)
            val result2 = habitManagementService.createHabit(request2)

            val foundHabit = habitManagementService.getHabit(result1.habit.id)
            assertNotNull(foundHabit)
            assertEquals("Утренняя зарядка", foundHabit!!.title)

            val userHabits = habitManagementService.getHabits(userId)
            assertEquals(2, userHabits.size)
            assertTrue(userHabits.any { it.title == "Утренняя зарядка" })
            assertTrue(userHabits.any { it.title == "Вечерняя прогулка" })
        }

        @Test
        @DisplayName("Получение несуществующей привычки возвращает null")
        fun `get non-existent habit returns null`() {
            val nonExistentId = HabitId.generate()

            val result = habitManagementService.getHabit(nonExistentId)

            assertNull(result)
        }
    }

    @Nested
    @DisplayName("UPDATE: Обновление привычки")
    inner class UpdateHabit {

        @Test
        @DisplayName("Обновление названия и длительности привычки")
        fun `update habit title and duration`() {
            val createRequest = createHabitRequest(title = "Старое название")
            val createdHabit = habitManagementService.createHabit(createRequest).habit

            val updateRequest = UpdateHabitRequest(
                title = "Новое название",
                duration = Duration.ofMinutes(45)
            )

            val updatedHabit = habitManagementService.updateHabit(createdHabit.id, updateRequest)

            assertEquals("Новое название", updatedHabit.title)
            assertEquals(Duration.ofMinutes(45), updatedHabit.duration)

            val savedHabit = habitRepository.findHabit(createdHabit.id)
            assertEquals("Новое название", savedHabit!!.title)
        }

        @Test
        @DisplayName("Обновление несуществующей привычки вызывает ошибку")
        fun `update non-existent habit throws exception`() {
            val nonExistentId = HabitId.generate()
            val updateRequest = UpdateHabitRequest(title = "Новое название")

            val exception = assertThrows(IllegalArgumentException::class.java) {
                habitManagementService.updateHabit(nonExistentId, updateRequest)
            }

            assertTrue(exception.message!!.contains("Habit not found"))
        }
    }

    @Nested
    @DisplayName("DELETE: Удаление привычки")
    inner class DeleteHabit {

        @Test
        @DisplayName("Удалённая привычка больше не доступна")
        fun `deleted habit is no longer available`() {
            val request = createHabitRequest(title = "Привычка для удаления")
            val createdHabit = habitManagementService.createHabit(request).habit

            assertNotNull(habitManagementService.getHabit(createdHabit.id))

            habitManagementService.deleteHabit(createdHabit.id)

            assertNull(habitManagementService.getHabit(createdHabit.id))

            val userHabits = habitManagementService.getHabits(userId)
            assertTrue(userHabits.none { it.id == createdHabit.id })
        }
    }

    private fun createHabitRequest(
        title: String,
        duration: Duration = Duration.ofHours(1)
    ): CreateHabitRequest {
        return CreateHabitRequest(
            userId = userId,
            title = title,
            duration = duration,
            recurrenceRule = RecurrenceRule.daily(today),
            flexibilityWindow = HabitFlexibilityWindow.workingHours()
        )
    }
}
