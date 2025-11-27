package ru.itmo.dws.calendar.core.port.input

import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.model.FocusTime
import ru.itmo.dws.calendar.core.domain.model.HabitConflict
import ru.itmo.dws.calendar.core.domain.model.Meeting
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface HabitConflictDetectionUseCase {

    fun detectConflictsForHabit(habitId: HabitId, date: LocalDate): List<HabitConflict>

    fun detectAllConflictsForUser(userId: UserId, date: LocalDate): List<HabitConflict>

    fun detectConflictsWithMeeting(meeting: Meeting): List<HabitConflict>

    fun detectConflictsWithFocusTime(focusTime: FocusTime): List<HabitConflict>

    fun hasConflicts(habitId: HabitId, date: LocalDate): Boolean
}
