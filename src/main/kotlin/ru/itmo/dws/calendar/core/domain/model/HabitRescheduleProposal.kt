package ru.itmo.dws.calendar.core.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

data class HabitRescheduleProposal(
    val id: String = UUID.randomUUID().toString(),
    val habitId: HabitId,
    val userId: UserId,
    val conflict: HabitConflict,
    val proposedSlots: List<ProposedSlot>,
    val alternativeDates: List<LocalDate> = emptyList(),
    val canSkipForDate: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant = Instant.now().plusSeconds(3600)
) {
    val hasAvailableSlots: Boolean
        get() = proposedSlots.isNotEmpty()

    val hasAlternativeDates: Boolean
        get() = alternativeDates.isNotEmpty()

    val bestSlot: ProposedSlot?
        get() = proposedSlots.maxByOrNull { it.score }

    fun isExpired(now: Instant = Instant.now()): Boolean = now.isAfter(expiresAt)

    fun getSlotByIndex(index: Int): ProposedSlot? = proposedSlots.getOrNull(index)
}

data class ProposedSlot(
    val timeSlot: TimeSlot,
    val score: Int,
    val tradeoffs: List<SlotTradeoff> = emptyList()
) {
    init {
        require(score in 0..100) { "Score must be between 0 and 100" }
    }
}

enum class SlotTradeoff(val description: String) {
    EARLIER_THAN_USUAL("Раньше обычного времени"),
    LATER_THAN_USUAL("Позже обычного времени"),
    SHORTER_BREAK_BEFORE("Короткий перерыв до события"),
    SHORTER_BREAK_AFTER("Короткий перерыв после события"),
    CLOSE_TO_WINDOW_START("Близко к началу допустимого окна"),
    CLOSE_TO_WINDOW_END("Близко к концу допустимого окна"),
    DIFFERENT_DAY("Перенос на другой день")
}

sealed class UserRescheduleDecision {
    data class AcceptSlot(val slotIndex: Int) : UserRescheduleDecision()
    data class CustomSlot(val timeSlot: TimeSlot) : UserRescheduleDecision()
    data class SkipForDate(val date: LocalDate) : UserRescheduleDecision()
    data class MoveToDifferentDay(val newDate: LocalDate, val timeSlot: TimeSlot) : UserRescheduleDecision()
    data object KeepConflict : UserRescheduleDecision()
}

data class HabitRescheduleResult(
    val habitId: HabitId,
    val status: RescheduleStatus,
    val previousTimeSlot: TimeSlot?,
    val newTimeSlot: TimeSlot?,
    val affectedDate: LocalDate,
    val message: String
) {
    val isSuccess: Boolean
        get() = status in listOf(
            RescheduleStatus.RESCHEDULED,
            RescheduleStatus.SKIPPED_FOR_DATE,
            RescheduleStatus.MOVED_TO_DIFFERENT_DAY
        )
}

enum class RescheduleStatus {
    RESCHEDULED,
    SKIPPED_FOR_DATE,
    MOVED_TO_DIFFERENT_DAY,
    KEPT_WITH_CONFLICT,
    FAILED_INVALID_SLOT,
    FAILED_SLOT_OCCUPIED,
    FAILED_PROPOSAL_EXPIRED
}
