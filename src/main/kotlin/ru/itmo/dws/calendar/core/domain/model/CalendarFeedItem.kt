package ru.itmo.dws.calendar.core.domain.model

import java.time.Duration
import java.time.LocalDate
import ru.itmo.dws.calendar.core.domain.valueobject.CalendarId
import ru.itmo.dws.calendar.core.domain.valueobject.FocusTimeId
import ru.itmo.dws.calendar.core.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.MeetingId
import ru.itmo.dws.calendar.core.domain.valueobject.RoomId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

data class CalendarFeedItem(
    val id: String,
    val timeSlot: TimeSlot,
    val title: String,
    val description: String?,
    val itemType: CalendarItemType,
    val source: EventSource,
    val externalEventId: String?,
    val capabilities: ItemCapabilities,
    val conflict: ConflictInfo? = null,
    val details: ItemDetails
) {
    val startTime get() = timeSlot.start
    val endTime get() = timeSlot.end
}

enum class CalendarItemType {
    HABIT,
    MEETING,
    FOCUS_TIME,
    EXTERNAL
}

enum class EventSource {
    INTERNAL_ONLY,
    EXTERNAL_ONLY,
    MIRRORED
}

data class ItemCapabilities(
    val canDelete: Boolean,
    val canReschedule: Boolean,
    val canEdit: Boolean
) {
    companion object {
        val READ_ONLY = ItemCapabilities(canDelete = false, canReschedule = false, canEdit = false)
        val FULL_CONTROL = ItemCapabilities(canDelete = true, canReschedule = true, canEdit = true)

        fun forHabitOccurrence() = ItemCapabilities(
            canDelete = true,
            canReschedule = true,
            canEdit = false
        )

        fun forMeeting(isCreator: Boolean) = ItemCapabilities(
            canDelete = isCreator,
            canReschedule = isCreator,
            canEdit = isCreator
        )

        fun forFocusTime() = ItemCapabilities(
            canDelete = true,
            canReschedule = true,
            canEdit = true
        )

        fun forExternalEvent() = READ_ONLY
    }
}

data class ConflictInfo(
    val conflictType: ConflictType,
    val conflictingEventIds: List<String>,
    val message: String? = null
)

enum class ConflictType {
    TIME_OVERLAP,
    FOCUS_TIME_VIOLATION,
    BUFFER_VIOLATION
}

sealed class ItemDetails {

    data class Habit(
        val habitId: HabitId,
        val occurrenceDate: LocalDate,
        val occurrenceStatus: OccurrenceStatus,
        val flexibilityWindow: HabitFlexibilityWindow,
        val duration: Duration
    ) : ItemDetails()

    data class Meeting(
        val meetingId: MeetingId,
        val participants: List<UserId>,
        val roomId: RoomId?,
        val bufferBefore: Duration?,
        val bufferAfter: Duration?,
        val isCreator: Boolean
    ) : ItemDetails()

    data class FocusTime(
        val focusTimeId: FocusTimeId,
        val isRecurring: Boolean
    ) : ItemDetails()

    data class External(
        val calendarId: CalendarId,
        val isAllDay: Boolean
    ) : ItemDetails()
}
