package ru.itmo.dws.calendar.core.port.output

import java.time.Duration
import ru.itmo.dws.calendar.core.domain.model.CalendarEvent
import ru.itmo.dws.calendar.core.domain.model.Conflict
import ru.itmo.dws.calendar.core.domain.model.ReconciliationResult
import ru.itmo.dws.calendar.core.domain.valueobject.SchedulingConstraints
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface SlotSuggestionStrategy {

    fun suggestSlots(
        participants: List<UserId>,
        duration: Duration,
        constraints: SchedulingConstraints,
        existingEvents: Map<UserId, List<CalendarEvent>>
    ): List<TimeSlot>

    fun resolveConflict(
        conflict: Conflict,
        context: ConflictResolutionContext
    ): ReconciliationResult

    fun strategyName(): String
}

data class ConflictResolutionContext(
    val allUserEvents: Map<UserId, List<CalendarEvent>>,
    val availableTimeRange: TimeSlot,
    val constraints: SchedulingConstraints,
    val priorityUsers: Set<UserId> = emptySet(),
    val allowCancellations: Boolean = false
)
