package ru.itmo.dws.calendar.port.output

import java.time.LocalDate
import ru.itmo.dws.calendar.domain.model.Habit
import ru.itmo.dws.calendar.domain.valueobject.HabitId
import ru.itmo.dws.calendar.domain.valueobject.UserId

interface HabitRepository {

    fun saveHabit(habit: Habit): HabitId

    fun findHabit(habitId: HabitId): Habit?

    fun findHabits(userId: UserId): List<Habit>

    fun findHabitsForDate(userId: UserId, date: LocalDate): List<Habit>

    fun updateHabit(habitId: HabitId, habit: Habit): Boolean

    fun deleteHabit(habitId: HabitId): Boolean
}
