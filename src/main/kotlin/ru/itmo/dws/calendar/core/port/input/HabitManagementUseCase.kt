package ru.itmo.dws.calendar.core.port.input

import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.model.CreateHabitRequest
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.model.HabitSchedulePlan
import ru.itmo.dws.calendar.core.domain.model.UpdateHabitRequest
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface HabitManagementUseCase {

    fun createHabit(request: CreateHabitRequest): HabitCreationResult

    fun getHabit(habitId: HabitId): Habit?

    fun getHabits(userId: UserId): List<Habit>

    fun getHabitsForDate(userId: UserId, date: LocalDate): List<Habit>

    fun updateHabit(habitId: HabitId, request: UpdateHabitRequest): Habit

    fun deleteHabit(habitId: HabitId)

    fun scheduleHabitForDate(habitId: HabitId, date: LocalDate, timeSlot: TimeSlot): Habit

    fun clearHabitScheduleForDate(habitId: HabitId, date: LocalDate): Habit

    fun planHabitSchedule(habitId: HabitId, weeks: Int = 4): HabitSchedulePlan

    fun syncHabitToExternalCalendar(habitId: HabitId, weeks: Int = 4): HabitSyncResult

    fun getHabitOccurrences(habitId: HabitId): List<HabitOccurrence>

    fun getHabitOccurrences(habitId: HabitId, startDate: LocalDate, endDate: LocalDate): List<HabitOccurrence>
}

data class HabitCreationResult(
    val habit: Habit,
    val scheduledSlot: TimeSlot?,
    val conflicts: List<ru.itmo.dws.calendar.core.domain.model.HabitConflict>,
    val status: HabitCreationStatus
) {
    val isSuccess: Boolean get() = status == HabitCreationStatus.CREATED_WITH_SLOT

    val hasConflicts: Boolean get() = conflicts.isNotEmpty()
}

enum class HabitCreationStatus {
    CREATED_WITH_SLOT,
    CREATED_WITHOUT_SLOT,
    CREATED_WITH_CONFLICTS
}

data class HabitSyncResult(
    val habitId: HabitId,
    val syncedCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val occurrences: List<HabitOccurrence>
) {
    val totalCount: Int get() = syncedCount + failedCount + skippedCount
    val isFullySuccessful: Boolean get() = failedCount == 0
}
