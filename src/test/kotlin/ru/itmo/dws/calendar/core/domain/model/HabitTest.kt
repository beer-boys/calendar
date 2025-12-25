package ru.itmo.dws.calendar.core.domain.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.itmo.dws.calendar.core.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.RecurrenceRule
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

@DisplayName("Habit - правило привычки")
class HabitTest {

    private val userId = UserId(UUID.randomUUID())
    private val today = LocalDate.now()

    @Nested
    @DisplayName("Создание привычки")
    inner class Creation {

        @Test
        @DisplayName("Привычка создаётся с корректными атрибутами: владелец, название, длительность, повторение, временное окно")
        fun `habit is created with all required attributes`() {
            val habitId = HabitId.generate()
            val title = "Обед"
            val duration = Duration.ofHours(1)
            val recurrenceRule = RecurrenceRule.weekly(
                startDate = today,
                daysOfWeek = setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
                )
            )
            val flexibilityWindow = HabitFlexibilityWindow(
                earliestTime = LocalTime.of(12, 0),
                latestTime = LocalTime.of(14, 0)
            )

            val habit = Habit(
                id = habitId,
                userId = userId,
                title = title,
                duration = duration,
                recurrenceRule = recurrenceRule,
                flexibilityWindow = flexibilityWindow
            )

            assertEquals(habitId, habit.id)
            assertEquals(userId, habit.userId)
            assertEquals(title, habit.title)
            assertEquals(duration, habit.duration)
            assertEquals(recurrenceRule, habit.recurrenceRule)
            assertEquals(flexibilityWindow, habit.flexibilityWindow)
        }
    }

    @Nested
    @DisplayName("Валидация")
    inner class Validation {

        @Test
        @DisplayName("Пустое или пробельное название привычки вызывает ошибку")
        fun `blank title throws exception`() {
            val exception = assertThrows(IllegalArgumentException::class.java) {
                createHabit(title = "   ")
            }

            assertEquals("Habit title cannot be blank", exception.message)
        }

        @Test
        @DisplayName("Нулевая или отрицательная длительность вызывает ошибку")
        fun `zero or negative duration throws exception`() {
            val zeroException = assertThrows(IllegalArgumentException::class.java) {
                createHabit(duration = Duration.ZERO)
            }
            assertEquals("Habit duration must be positive", zeroException.message)

            val negativeException = assertThrows(IllegalArgumentException::class.java) {
                createHabit(duration = Duration.ofMinutes(-30))
            }
            assertEquals("Habit duration must be positive", negativeException.message)
        }
    }

    private fun createHabit(
        title: String = "Тестовая привычка",
        duration: Duration = Duration.ofHours(1)
    ): Habit {
        return Habit(
            id = HabitId.generate(),
            userId = userId,
            title = title,
            duration = duration,
            recurrenceRule = RecurrenceRule.daily(today),
            flexibilityWindow = HabitFlexibilityWindow.workingHours()
        )
    }
}
