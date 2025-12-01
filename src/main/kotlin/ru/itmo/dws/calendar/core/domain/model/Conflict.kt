package ru.itmo.dws.calendar.core.domain.model

import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

data class Conflict(
    val conflictingEvents: List<CalendarEvent>,
    val affectedUsers: Set<UserId>,
    val timeSlot: TimeSlot,
    val conflictType: ConflictType
) {
    init {
        require(conflictingEvents.size >= 2) {
            "Conflict must involve at least 2 events"
        }
        require(affectedUsers.isNotEmpty()) {
            "Conflict must affect at least one user"
        }
    }

    fun affectsUser(userId: UserId): Boolean {
        return affectedUsers.contains(userId)
    }

    fun eventsForUser(userId: UserId): List<CalendarEvent> {
        return conflictingEvents.filter { it.hasParticipant(userId) }
    }

    enum class ConflictType {
        MEETING_OVERLAP,
        HABIT_CONFLICT,
        FOCUS_TIME_VIOLATION,
        BUFFER_TIME_VIOLATION
    }

    companion object {
        fun detectConflicts(events: List<CalendarEvent>): List<Conflict> {
            val conflicts = mutableListOf<Conflict>()

            for (i in events.indices) {
                for (j in i + 1 until events.size) {
                    val event1 = events[i]
                    val event2 = events[j]

                    if (event1.conflictsWith(event2)) {
                        val affectedUsers = (event1.participants + event2.participants + event1.owner + event2.owner).toSet()
                        val overlapStart = maxOf(event1.timeSlot.start, event2.timeSlot.start)
                        val overlapEnd = minOf(event1.timeSlot.end, event2.timeSlot.end)

                        conflicts.add(
                            Conflict(
                                conflictingEvents = listOf(event1, event2),
                                affectedUsers = affectedUsers,
                                timeSlot = TimeSlot(overlapStart, overlapEnd),
                                conflictType = ConflictType.MEETING_OVERLAP
                            )
                        )
                    }
                }
            }

            return conflicts
        }
    }
}
