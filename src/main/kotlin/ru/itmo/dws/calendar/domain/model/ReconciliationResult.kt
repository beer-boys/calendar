package ru.itmo.dws.calendar.domain.model

import ru.itmo.dws.calendar.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.domain.valueobject.UserId

data class ReconciliationResult(
    val resolvedMeetings: List<ResolvedMeeting>,
    val unresolvedConflicts: List<Conflict>,
    val affectedUsers: Map<UserId, UserImpact>,
    val success: Boolean
) {
    fun isFullyResolved(): Boolean {
        return unresolvedConflicts.isEmpty()
    }

    fun isPartiallyResolved(): Boolean {
        return resolvedMeetings.isNotEmpty() && unresolvedConflicts.isNotEmpty()
    }

    fun rescheduledCount(): Int {
        return resolvedMeetings.count { it.wasRescheduled }
    }

    companion object {
        fun success(
            resolvedMeetings: List<ResolvedMeeting>,
            affectedUsers: Map<UserId, UserImpact>
        ): ReconciliationResult {
            return ReconciliationResult(
                resolvedMeetings = resolvedMeetings,
                unresolvedConflicts = emptyList(),
                affectedUsers = affectedUsers,
                success = true
            )
        }

        fun failure(unresolvedConflicts: List<Conflict>): ReconciliationResult {
            return ReconciliationResult(
                resolvedMeetings = emptyList(),
                unresolvedConflicts = unresolvedConflicts,
                affectedUsers = emptyMap(),
                success = false
            )
        }

        fun partial(
            resolvedMeetings: List<ResolvedMeeting>,
            unresolvedConflicts: List<Conflict>,
            affectedUsers: Map<UserId, UserImpact>
        ): ReconciliationResult {
            return ReconciliationResult(
                resolvedMeetings = resolvedMeetings,
                unresolvedConflicts = unresolvedConflicts,
                affectedUsers = affectedUsers,
                success = false
            )
        }
    }
}

data class ResolvedMeeting(
    val originalEvent: CalendarEvent,
    val newTimeSlot: TimeSlot?,
    val wasRescheduled: Boolean,
    val wasCancelled: Boolean = false
) {
    init {
        require(wasRescheduled || wasCancelled || newTimeSlot == null) {
            "Meeting must be either rescheduled, cancelled, or unchanged"
        }
        if (wasRescheduled) {
            require(newTimeSlot != null) { "Rescheduled meeting must have a new time slot" }
        }
    }
}

data class UserImpact(
    val lostTimeSlots: List<TimeSlot>,
    val gainedTimeSlots: List<TimeSlot>,
    val rescheduledMeetings: Int,
    val cancelledMeetings: Int = 0
) {
    companion object {
        fun noImpact(): UserImpact {
            return UserImpact(
                lostTimeSlots = emptyList(),
                gainedTimeSlots = emptyList(),
                rescheduledMeetings = 0,
                cancelledMeetings = 0
            )
        }
    }
}
