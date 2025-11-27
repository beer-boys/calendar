package ru.itmo.dws.calendar.core.port.input

import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.model.EventConflict
import ru.itmo.dws.calendar.core.domain.model.EventType
import ru.itmo.dws.calendar.core.domain.model.SchedulableEvent
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface ConflictDetectionUseCase {

    fun detectAllConflictsForUser(userId: UserId, date: LocalDate): List<EventConflict>

    fun detectConflictsForEvent(event: SchedulableEvent, date: LocalDate): List<EventConflict>

    fun detectConflictsInTimeSlot(
        userId: UserId,
        timeSlot: TimeSlot,
        excludeEventId: String? = null
    ): List<EventConflict>

    fun detectConflictsBetweenTypes(
        userId: UserId,
        date: LocalDate,
        sourceType: EventType,
        targetType: EventType
    ): List<EventConflict>

    fun hasConflicts(event: SchedulableEvent, date: LocalDate): Boolean
}
