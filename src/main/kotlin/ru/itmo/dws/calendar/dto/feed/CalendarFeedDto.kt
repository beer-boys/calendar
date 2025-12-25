package ru.itmo.dws.calendar.dto.feed

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import ru.itmo.dws.calendar.core.domain.model.CalendarFeedItem
import ru.itmo.dws.calendar.core.domain.model.CalendarItemType
import ru.itmo.dws.calendar.core.domain.model.ConflictInfo
import ru.itmo.dws.calendar.core.domain.model.ConflictType
import ru.itmo.dws.calendar.core.domain.model.EventSource
import ru.itmo.dws.calendar.core.domain.model.ItemCapabilities
import ru.itmo.dws.calendar.core.domain.model.ItemDetails
import ru.itmo.dws.calendar.core.port.input.CalendarFeedResult

data class CalendarFeedResponseDto(
    val events: List<CalendarFeedItemDto>,
    val period: TimeRangeDto,
    val totalCount: Int,
    val hasConflicts: Boolean
) {
    companion object {
        fun fromDomain(result: CalendarFeedResult): CalendarFeedResponseDto {
            return CalendarFeedResponseDto(
                events = result.events.map { CalendarFeedItemDto.fromDomain(it) },
                period = TimeRangeDto(
                    start = result.period.start,
                    end = result.period.end
                ),
                totalCount = result.totalCount,
                hasConflicts = result.hasConflicts
            )
        }
    }
}

data class TimeRangeDto(
    val start: ZonedDateTime,
    val end: ZonedDateTime
)

data class CalendarFeedItemDto(
    val id: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val title: String,
    val description: String?,
    val itemType: CalendarItemType,
    val source: EventSource,
    val externalEventId: String?,
    val capabilities: ItemCapabilitiesDto,
    val conflict: ConflictInfoDto?,
    val details: ItemDetailsDto
) {
    companion object {
        fun fromDomain(item: CalendarFeedItem): CalendarFeedItemDto {
            return CalendarFeedItemDto(
                id = item.id,
                startTime = item.startTime,
                endTime = item.endTime,
                title = item.title,
                description = item.description,
                itemType = item.itemType,
                source = item.source,
                externalEventId = item.externalEventId,
                capabilities = ItemCapabilitiesDto.fromDomain(item.capabilities),
                conflict = item.conflict?.let { ConflictInfoDto.fromDomain(it) },
                details = ItemDetailsDto.fromDomain(item.details)
            )
        }
    }
}

data class ItemCapabilitiesDto(
    val canDelete: Boolean,
    val canReschedule: Boolean,
    val canEdit: Boolean
) {
    companion object {
        fun fromDomain(capabilities: ItemCapabilities): ItemCapabilitiesDto {
            return ItemCapabilitiesDto(
                canDelete = capabilities.canDelete,
                canReschedule = capabilities.canReschedule,
                canEdit = capabilities.canEdit
            )
        }
    }
}

data class ConflictInfoDto(
    val conflictType: ConflictType,
    val conflictingEventIds: List<String>,
    val message: String?
) {
    companion object {
        fun fromDomain(conflict: ConflictInfo): ConflictInfoDto {
            return ConflictInfoDto(
                conflictType = conflict.conflictType,
                conflictingEventIds = conflict.conflictingEventIds,
                message = conflict.message
            )
        }
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ItemDetailsDto.HabitDto::class, name = "HABIT"),
    JsonSubTypes.Type(value = ItemDetailsDto.MeetingDto::class, name = "MEETING"),
    JsonSubTypes.Type(value = ItemDetailsDto.FocusTimeDto::class, name = "FOCUS_TIME"),
    JsonSubTypes.Type(value = ItemDetailsDto.ExternalDto::class, name = "EXTERNAL")
)
sealed class ItemDetailsDto {

    data class HabitDto(
        val habitId: UUID,
        val occurrenceDate: LocalDate,
        val occurrenceStatus: String,
        val flexibilityWindowStart: String,
        val flexibilityWindowEnd: String,
        val durationMinutes: Long
    ) : ItemDetailsDto()

    data class MeetingDto(
        val meetingId: UUID,
        val participants: List<UUID>,
        val roomId: UUID?,
        val bufferBeforeMinutes: Long?,
        val bufferAfterMinutes: Long?,
        val isCreator: Boolean
    ) : ItemDetailsDto()

    data class FocusTimeDto(
        val focusTimeId: UUID,
        val isRecurring: Boolean
    ) : ItemDetailsDto()

    data class ExternalDto(
        val calendarId: String,
        val isAllDay: Boolean
    ) : ItemDetailsDto()

    companion object {
        fun fromDomain(details: ItemDetails): ItemDetailsDto {
            return when (details) {
                is ItemDetails.Habit -> HabitDto(
                    habitId = details.habitId.value,
                    occurrenceDate = details.occurrenceDate,
                    occurrenceStatus = details.occurrenceStatus.name,
                    flexibilityWindowStart = details.flexibilityWindow.earliestTime.toString(),
                    flexibilityWindowEnd = details.flexibilityWindow.latestTime.toString(),
                    durationMinutes = details.duration.toMinutes()
                )

                is ItemDetails.Meeting -> MeetingDto(
                    meetingId = details.meetingId.value,
                    participants = details.participants.map { it.value },
                    roomId = details.roomId?.value,
                    bufferBeforeMinutes = details.bufferBefore?.toMinutes(),
                    bufferAfterMinutes = details.bufferAfter?.toMinutes(),
                    isCreator = details.isCreator
                )

                is ItemDetails.FocusTime -> FocusTimeDto(
                    focusTimeId = details.focusTimeId.value,
                    isRecurring = details.isRecurring
                )

                is ItemDetails.External -> ExternalDto(
                    calendarId = details.calendarId.value,
                    isAllDay = details.isAllDay
                )
            }
        }
    }
}
