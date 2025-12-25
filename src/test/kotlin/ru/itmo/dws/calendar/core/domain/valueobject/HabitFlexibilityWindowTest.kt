package ru.itmo.dws.calendar.core.domain.valueobject

import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("HabitFlexibilityWindow - дневное временное окно привычки")
class HabitFlexibilityWindowTest {

    @Nested
    @DisplayName("Проверка временного окна")
    inner class TimeWindow {

        @Test
        @DisplayName("Время внутри окна 12:00-14:00 проходит проверку, за пределами - нет")
        fun `time within window passes validation`() {
            val window = HabitFlexibilityWindow(
                earliestTime = LocalTime.of(12, 0),
                latestTime = LocalTime.of(14, 0)
            )

            assertTrue(window.isWithinWindow(LocalTime.of(12, 0)))
            assertTrue(window.isWithinWindow(LocalTime.of(13, 0)))
            assertTrue(window.isWithinWindow(LocalTime.of(14, 0)))

            assertFalse(window.isWithinWindow(LocalTime.of(11, 59)))
            assertFalse(window.isWithinWindow(LocalTime.of(14, 1)))
        }

        @Test
        @DisplayName("Фабричные методы создают корректные окна")
        fun `factory methods create correct windows`() {
            val workingHours = HabitFlexibilityWindow.workingHours()
            assertEquals(LocalTime.of(9, 0), workingHours.earliestTime)
            assertEquals(LocalTime.of(18, 0), workingHours.latestTime)

            val morning = HabitFlexibilityWindow.morning()
            assertEquals(LocalTime.of(6, 0), morning.earliestTime)
            assertEquals(LocalTime.of(12, 0), morning.latestTime)
        }
    }

    @Nested
    @DisplayName("Валидация")
    inner class Validation {

        @Test
        @DisplayName("latestTime не позже earliestTime вызывает ошибку")
        fun `latest time must be after earliest time`() {
            val exception = assertThrows(IllegalArgumentException::class.java) {
                HabitFlexibilityWindow(
                    earliestTime = LocalTime.of(14, 0),
                    latestTime = LocalTime.of(12, 0)
                )
            }

            assertTrue(exception.message!!.contains("Latest time must be after earliest time"))
        }
    }
}
