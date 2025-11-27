package ru.itmo.dws.calendar.core.repository

import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.HabitRepository

open class InMemoryHabitRepository : HabitRepository {
    private val habits = ConcurrentHashMap<HabitId, Habit>()

    override fun saveHabit(habit: Habit): HabitId {
        habits[habit.id] = habit
        return habit.id
    }

    override fun findHabit(habitId: HabitId): Habit? = habits[habitId]

    override fun findHabits(userId: UserId): List<Habit> =
        habits.values.filter { it.userId == userId }

    override fun findHabitsForDate(userId: UserId, date: LocalDate): List<Habit> =
        habits.values.filter { it.userId == userId && it.shouldOccurOn(date) }

    override fun updateHabit(habitId: HabitId, habit: Habit): Boolean {
        if (habits.containsKey(habitId)) {
            habits[habitId] = habit
            return true
        }
        return false
    }

    override fun deleteHabit(habitId: HabitId): Boolean = habits.remove(habitId) != null
}
