package ru.itmo.dws.calendar.core.domain.model

import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot

data class HabitOccurrence(
    val habitId: HabitId,
    val date: LocalDate,
    val status: OccurrenceStatus,
    val timeSlot: TimeSlot? = null,
    val reason: String? = null,
    val externalEventId: String? = null
) {
    val isScheduled: Boolean get() = status == OccurrenceStatus.SCHEDULED
    val isSynced: Boolean get() = externalEventId != null

    fun withExternalEventId(eventId: String): HabitOccurrence = copy(externalEventId = eventId)
}

enum class OccurrenceStatus {
    SCHEDULED,
    UNSCHEDULED,
    CANCELLED
}

data class HabitSchedulePlan(
    val habitId: HabitId,
    val habitTitle: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val occurrences: List<HabitOccurrence>
) {
    val scheduledCount: Int get() = occurrences.count { it.isScheduled }
    val unscheduledCount: Int get() = occurrences.count { it.status == OccurrenceStatus.UNSCHEDULED }
    val totalCount: Int get() = occurrences.size
}
