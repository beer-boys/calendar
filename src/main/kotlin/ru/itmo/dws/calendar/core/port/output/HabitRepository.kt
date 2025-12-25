package ru.itmo.dws.calendar.core.port.output

import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface HabitRepository {

    fun saveHabit(habit: Habit): HabitId

    fun findHabit(habitId: HabitId): Habit?

    fun findHabits(userId: UserId): List<Habit>

    fun findByUserId(userId: UserId): List<Habit> = findHabits(userId)

    fun findByIds(habitIds: List<HabitId>): List<Habit>

    fun findHabitsForDate(userId: UserId, date: LocalDate): List<Habit>

    fun findAllHabits(): List<Habit>

    fun updateHabit(habitId: HabitId, habit: Habit): Boolean

    fun deleteHabit(habitId: HabitId): Boolean
}
