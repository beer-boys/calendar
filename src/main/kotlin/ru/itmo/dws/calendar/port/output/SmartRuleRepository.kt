package ru.itmo.dws.calendar.port.output

import java.time.LocalDate
import ru.itmo.dws.calendar.domain.model.FocusTime
import ru.itmo.dws.calendar.domain.model.Habit
import ru.itmo.dws.calendar.domain.model.Meeting
import ru.itmo.dws.calendar.domain.valueobject.*

// TODO think if needs to be split up as several ports
interface SmartRuleRepository {

    fun saveHabit(habit: Habit): HabitId

    fun findHabit(habitId: HabitId): Habit?

    fun findHabits(userId: UserId): List<Habit>

    fun findHabitsForDate(userId: UserId, date: LocalDate): List<Habit>

    fun updateHabit(habitId: HabitId, habit: Habit): Boolean

    fun deleteHabit(habitId: HabitId): Boolean


    fun saveFocusTime(focusTime: FocusTime): FocusTimeId

    fun findFocusTime(focusTimeId: FocusTimeId): FocusTime?

    fun findFocusTimes(userId: UserId, timeRange: TimeSlot): List<FocusTime>

    fun deleteFocusTime(focusTimeId: FocusTimeId): Boolean


    fun saveMeeting(meeting: Meeting): MeetingId

    fun findMeeting(meetingId: MeetingId): Meeting?

    fun findMeetings(userId: UserId, timeRange: TimeSlot): List<Meeting>

    fun updateMeeting(meetingId: MeetingId, meeting: Meeting): Boolean

    fun deleteMeeting(meetingId: MeetingId): Boolean
}
