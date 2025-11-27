package ru.itmo.dws.calendar.port.output

import ru.itmo.dws.calendar.domain.model.Conflict
import ru.itmo.dws.calendar.domain.model.Habit
import ru.itmo.dws.calendar.domain.model.Meeting
import ru.itmo.dws.calendar.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.domain.valueobject.UserId

interface NotificationProvider {

    fun notifyMeetingRescheduled(meeting: Meeting, oldTimeSlot: TimeSlot, newTimeSlot: TimeSlot)

    fun notifyMeetingConflict(userId: UserId, conflict: Conflict)

    fun notifyHabitMoved(habit: Habit, oldTimeSlot: TimeSlot, newTimeSlot: TimeSlot)

    fun notifyHabitReschedulingFailed(userId: UserId, habit: Habit, reason: String)

    fun notifyMeetingInvitation(meeting: Meeting, invitedUsers: List<UserId>)

    fun notifyUpcomingMeeting(userId: UserId, meeting: Meeting, minutesBeforeMeeting: Int)

    fun notifyMeetingCancelled(meeting: Meeting, reason: String? = null)
}
