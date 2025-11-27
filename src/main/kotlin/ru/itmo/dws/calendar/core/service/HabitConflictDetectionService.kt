package ru.itmo.dws.calendar.core.service

import java.time.LocalDate
import java.time.ZoneId
import ru.itmo.dws.calendar.core.domain.model.ConflictingEvent
import ru.itmo.dws.calendar.core.domain.model.FocusTime
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.model.HabitConflict
import ru.itmo.dws.calendar.core.domain.model.Meeting
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.MeetingId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.input.HabitConflictDetectionUseCase
import ru.itmo.dws.calendar.core.port.output.CalendarProvider
import ru.itmo.dws.calendar.core.port.output.FocusTimeRepository
import ru.itmo.dws.calendar.core.port.output.HabitRepository
import ru.itmo.dws.calendar.core.port.output.MeetingRepository

class HabitConflictDetectionService(
    private val habitRepository: HabitRepository,
    private val meetingRepository: MeetingRepository,
    private val focusTimeRepository: FocusTimeRepository,
    private val calendarProvider: CalendarProvider,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : HabitConflictDetectionUseCase {

    override fun detectConflictsForHabit(habitId: HabitId, date: LocalDate): List<HabitConflict> {
        val habit = habitRepository.findHabit(habitId) ?: return emptyList()

        if (!habit.shouldOccurOn(date)) return emptyList()

        val habitSlot = habit.effectiveTimeSlot() ?: return emptyList()

        return detectConflictsForHabitInternal(habit, date, habitSlot)
    }

    override fun detectAllConflictsForUser(userId: UserId, date: LocalDate): List<HabitConflict> {
        val habits = habitRepository.findHabitsForDate(userId, date)
        val conflicts = mutableListOf<HabitConflict>()

        habits.forEach { habit ->
            val habitSlot = habit.effectiveTimeSlot()
            if (habitSlot != null) {
                conflicts.addAll(detectConflictsForHabitInternal(habit, date, habitSlot))
            }
        }

        return conflicts
    }

    override fun detectConflictsWithMeeting(meeting: Meeting): List<HabitConflict> {
        val meetingDate = meeting.timeSlot.start.toLocalDate()
        val conflicts = mutableListOf<HabitConflict>()

        meeting.participants.forEach { userId ->
            val habits = habitRepository.findHabitsForDate(userId, meetingDate)
            habits.forEach { habit ->
                if (habit.conflictsWith(meeting)) {
                    HabitConflict.fromMeeting(habit, meeting, meetingDate)?.let {
                        conflicts.add(it)
                    }
                }
            }
        }

        return conflicts
    }

    override fun detectConflictsWithFocusTime(focusTime: FocusTime): List<HabitConflict> {
        val focusDate = focusTime.timeSlot.start.toLocalDate()
        val habits = habitRepository.findHabitsForDate(focusTime.userId, focusDate)
        val conflicts = mutableListOf<HabitConflict>()

        habits.forEach { habit ->
            if (habit.conflictsWith(focusTime)) {
                HabitConflict.fromFocusTime(habit, focusTime, focusDate)?.let {
                    conflicts.add(it)
                }
            }
        }

        return conflicts
    }

    override fun hasConflicts(habitId: HabitId, date: LocalDate): Boolean {
        return detectConflictsForHabit(habitId, date).isNotEmpty()
    }

    private fun detectConflictsForHabitInternal(
        habit: Habit,
        date: LocalDate,
        habitSlot: TimeSlot
    ): List<HabitConflict> {
        val conflicts = mutableListOf<HabitConflict>()
        val dayTimeSlot = createDayTimeSlot(date)

        conflicts.addAll(detectMeetingConflicts(habit, date, dayTimeSlot))
        conflicts.addAll(detectFocusTimeConflicts(habit, date, dayTimeSlot))
        conflicts.addAll(detectHabitConflicts(habit, date))
        conflicts.addAll(detectCalendarEventConflicts(habit, date, habitSlot, dayTimeSlot))

        return conflicts
    }

    private fun detectMeetingConflicts(
        habit: Habit,
        date: LocalDate,
        dayTimeSlot: TimeSlot
    ): List<HabitConflict> {
        val meetings = meetingRepository.findMeetings(habit.userId, dayTimeSlot)
        return meetings.mapNotNull { meeting ->
            if (habit.conflictsWith(meeting)) {
                HabitConflict.fromMeeting(habit, meeting, date)
            } else {
                null
            }
        }
    }

    private fun detectFocusTimeConflicts(
        habit: Habit,
        date: LocalDate,
        dayTimeSlot: TimeSlot
    ): List<HabitConflict> {
        val focusTimes = focusTimeRepository.findFocusTimes(habit.userId, dayTimeSlot)
        return focusTimes.mapNotNull { focusTime ->
            if (habit.conflictsWith(focusTime)) {
                HabitConflict.fromFocusTime(habit, focusTime, date)
            } else {
                null
            }
        }
    }

    private fun detectHabitConflicts(habit: Habit, date: LocalDate): List<HabitConflict> {
        val otherHabits = habitRepository.findHabitsForDate(habit.userId, date)
            .filter { it.id != habit.id }
        return otherHabits.mapNotNull { otherHabit ->
            if (habit.conflictsWith(otherHabit)) {
                HabitConflict.fromHabit(habit, otherHabit, date)
            } else {
                null
            }
        }
    }

    private fun detectCalendarEventConflicts(
        habit: Habit,
        date: LocalDate,
        habitSlot: TimeSlot,
        dayTimeSlot: TimeSlot
    ): List<HabitConflict> {
        val calendarEvents = try {
            calendarProvider.getEvents(habit.userId, dayTimeSlot)
        } catch (e: Exception) {
            return emptyList()
        }

        return calendarEvents
            .filter { it.isBlocking() && habitSlot.overlapsWith(it.timeSlot) }
            .map { event ->
                HabitConflict(
                    habitId = habit.id,
                    habitTitle = habit.title,
                    habitTimeSlot = habitSlot,
                    habitPriority = habit.priority,
                    conflictingEvent = ConflictingEvent.MeetingEvent(
                        Meeting(
                            id = MeetingId.of(event.externalId.take(36).padEnd(36, '0')),
                            creator = event.owner,
                            timeSlot = event.timeSlot,
                            participants = event.participants.ifEmpty { listOf(event.owner) },
                            title = event.title,
                            description = event.description
                        )
                    ),
                    conflictType = HabitConflict.ConflictType.MEETING_OVERLAP,
                    affectedDate = date,
                    userId = habit.userId
                )
            }
    }

    private fun createDayTimeSlot(date: LocalDate): TimeSlot {
        val startOfDay = date.atStartOfDay(zoneId)
        val endOfDay = date.plusDays(1).atStartOfDay(zoneId)
        return TimeSlot(startOfDay, endOfDay)
    }
}
