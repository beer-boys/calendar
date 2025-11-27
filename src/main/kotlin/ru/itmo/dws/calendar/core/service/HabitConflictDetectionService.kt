package ru.itmo.dws.calendar.core.service

import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.model.FocusTime
import ru.itmo.dws.calendar.core.domain.model.HabitConflict
import ru.itmo.dws.calendar.core.domain.model.Meeting
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.model.toHabitConflict
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.input.HabitConflictDetectionUseCase
import ru.itmo.dws.calendar.core.port.output.FocusTimeRepository
import ru.itmo.dws.calendar.core.port.output.HabitRepository
import ru.itmo.dws.calendar.core.port.output.MeetingRepository

class HabitConflictDetectionService(
    private val habitRepository: HabitRepository,
    private val meetingRepository: MeetingRepository,
    private val focusTimeRepository: FocusTimeRepository,
    private val conflictDetector: ConflictDetector = ConflictDetector()
) : HabitConflictDetectionUseCase {

    override fun detectConflictsForHabit(habitId: HabitId, date: LocalDate): List<HabitConflict> {
        val habit = habitRepository.findHabit(habitId) ?: return emptyList()

        if (!habit.shouldOccurOn(date)) return emptyList()
        if (habit.effectiveTimeSlot() == null) return emptyList()

        val allEvents = collectAllEventsForUser(habit.userId, date)
        val conflicts = conflictDetector.detectConflictsFor(habit, allEvents, date)

        return conflicts.mapNotNull { it.toHabitConflict() }
    }

    override fun detectAllConflictsForUser(userId: UserId, date: LocalDate): List<HabitConflict> {
        val habits = habitRepository.findHabitsForDate(userId, date)
            .filter { it.effectiveTimeSlot() != null }

        if (habits.isEmpty()) return emptyList()

        val allEvents = collectAllEventsForUser(userId, date)
        val conflicts = mutableListOf<HabitConflict>()

        habits.forEach { habit ->
            val habitConflicts = conflictDetector.detectConflictsFor(habit, allEvents, date)
            conflicts.addAll(habitConflicts.mapNotNull { it.toHabitConflict() })
        }

        return conflicts
    }

    override fun detectConflictsWithMeeting(meeting: Meeting): List<HabitConflict> {
        val meetingDate = meeting.timeSlot.start.toLocalDate()
        val conflicts = mutableListOf<HabitConflict>()

        meeting.participants.forEach { userId ->
            val habits = habitRepository.findHabitsForDate(userId, meetingDate)
                .filter { it.effectiveTimeSlot() != null }

            habits.forEach { habit ->
                val eventConflicts = conflictDetector.detectConflictsFor(
                    event = habit,
                    otherEvents = listOf(meeting),
                    date = meetingDate
                )
                conflicts.addAll(eventConflicts.mapNotNull { it.toHabitConflict() })
            }
        }

        return conflicts
    }

    override fun detectConflictsWithFocusTime(focusTime: FocusTime): List<HabitConflict> {
        val focusDate = focusTime.timeSlot.start.toLocalDate()
        val habits = habitRepository.findHabitsForDate(focusTime.userId, focusDate)
            .filter { it.effectiveTimeSlot() != null }

        val conflicts = mutableListOf<HabitConflict>()

        habits.forEach { habit ->
            val eventConflicts = conflictDetector.detectConflictsFor(
                event = habit,
                otherEvents = listOf(focusTime),
                date = focusDate
            )
            conflicts.addAll(eventConflicts.mapNotNull { it.toHabitConflict() })
        }

        return conflicts
    }

    override fun hasConflicts(habitId: HabitId, date: LocalDate): Boolean {
        return detectConflictsForHabit(habitId, date).isNotEmpty()
    }

    private fun collectAllEventsForUser(userId: UserId, date: LocalDate): List<SchedulableEvent> {
        val dayTimeSlot = conflictDetector.createDayTimeSlot(date)
        val events = mutableListOf<SchedulableEvent>()

        events.addAll(meetingRepository.findMeetings(userId, dayTimeSlot))
        events.addAll(habitRepository.findHabitsForDate(userId, date))
        events.addAll(focusTimeRepository.findFocusTimes(userId, dayTimeSlot))

        return events
    }
}
