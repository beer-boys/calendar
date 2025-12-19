package ru.itmo.dws.calendar.core.domain.valueobject

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RecurrenceRule - правило повторения привычки")
class RecurrenceRuleTest {

    private val monday = LocalDate.of(2025, 1, 6)

    @Nested
    @DisplayName("Правила повторения")
    inner class Recurrence {

        @Test
        @DisplayName("DAILY: привычка повторяется каждый день")
        fun `daily habit occurs every day`() {
            val rule = RecurrenceRule.daily(startDate = monday)

            assertTrue(rule.occursOn(monday))
            assertTrue(rule.occursOn(monday.plusDays(1)))
            assertTrue(rule.occursOn(monday.plusDays(6)))
            assertTrue(rule.occursOn(monday.plusDays(30)))
        }

        @Test
        @DisplayName("WEEKLY по будням: привычка только Пн-Пт, не срабатывает в выходные")
        fun `weekly weekdays habit occurs only on workdays`() {
            val workdays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )
            val rule = RecurrenceRule.weekly(startDate = monday, daysOfWeek = workdays)

            assertTrue(rule.occursOn(monday))
            assertTrue(rule.occursOn(monday.plusDays(1)))
            assertTrue(rule.occursOn(monday.plusDays(4)))

            assertFalse(rule.occursOn(monday.plusDays(5)))
            assertFalse(rule.occursOn(monday.plusDays(6)))

            assertTrue(rule.occursOn(monday.plusDays(7)))
        }

        @Test
        @DisplayName("Привычка не срабатывает до startDate и после endDate")
        fun `habit respects date boundaries`() {
            val startDate = monday
            val endDate = monday.plusDays(7)
            val rule = RecurrenceRule.daily(startDate = startDate, endDate = endDate)

            assertFalse(rule.occursOn(monday.minusDays(1)))
            assertTrue(rule.occursOn(monday))
            assertTrue(rule.occursOn(endDate))
            assertFalse(rule.occursOn(endDate.plusDays(1)))
        }
    }

    @Nested
    @DisplayName("Валидация")
    inner class Validation {

        @Test
        @DisplayName("WEEKLY без указания дней недели вызывает ошибку")
        fun `weekly frequency requires days of week`() {
            val exception = assertThrows(IllegalArgumentException::class.java) {
                RecurrenceRule(
                    frequency = RecurrenceRule.Frequency.WEEKLY,
                    daysOfWeek = emptySet(),
                    startDate = monday
                )
            }

            assertEquals("Days of week must be specified for weekly frequency", exception.message)
        }

        @Test
        @DisplayName("endDate раньше startDate вызывает ошибку")
        fun `end date before start date throws exception`() {
            val exception = assertThrows(IllegalArgumentException::class.java) {
                RecurrenceRule.daily(
                    startDate = monday,
                    endDate = monday.minusDays(1)
                )
            }

            assertEquals("End date cannot be before start date", exception.message)
        }
    }
}
