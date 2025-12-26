package ru.itmo.dws.calendar.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.itmo.dws.calendar.configuration.BasePath
import ru.itmo.dws.calendar.core.domain.model.MeetingRoom
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomSearchCriteria
import ru.itmo.dws.calendar.core.domain.valueobject.room.RoomFeature
import ru.itmo.dws.calendar.core.port.input.room.MeetingRoomQueryUseCase
import ru.itmo.dws.calendar.dto.TimeSlotDto
import ru.itmo.dws.calendar.dto.room.MeetingRoomResponseDto
import ru.itmo.dws.calendar.mapper.toResponseDto
import ru.itmo.dws.calendar.model.User

@RestController
@RequestMapping("${BasePath.BASE}/meeting-rooms")
@Tag(name = "Meeting Rooms", description = "API для работы с переговорками")
@Suppress("TooManyFunctions")
class MeetingRoomController(private val meetingRoomQueryUseCase: MeetingRoomQueryUseCase) {

    @GetMapping
    @Operation(
        summary = "Получение переговорок",
        description = "Получение переговорок с учетом фильтров",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Успех"),
        ApiResponse(responseCode = "401", description = "Не авторизован", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = [Content()]),
    )
    fun findRooms(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false) minCapacity: Int?,
        @RequestParam(required = false) locationQuery: String?,
        @RequestParam(required = false) floor: Int?,
        @RequestParam(required = false) building: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) requiredFeatures: List<String>,
    ): List<MeetingRoomResponseDto> {
        val criteria = buildCriteria(
            minCapacity,
            locationQuery,
            floor,
            building,
            status,
            requiredFeatures,
        )

        return meetingRoomQueryUseCase.findRooms(criteria, UserId.of(user.id)).map { it.toResponseDto() }
    }

    @GetMapping("/available")
    @Operation(
        summary = "Получение доступных переговорок",
        description = "Получение доступных переговорок с учетом фильтров",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Успех"),
        ApiResponse(responseCode = "401", description = "Не авторизован", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = [Content()]),
    )
    fun findAvailableRooms(
        @AuthenticationPrincipal user: User,
        @RequestParam startTime: ZonedDateTime,
        @RequestParam endTime: ZonedDateTime,
        @RequestParam(required = false) minCapacity: Int?,
        @RequestParam(required = false) locationQuery: String?,
        @RequestParam(required = false) floor: Int?,
        @RequestParam(required = false) building: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) requiredFeatures: List<String>,
    ): List<MeetingRoomResponseDto> {
        val slot = TimeSlot(startTime, endTime)
        val criteria = buildCriteria(
            minCapacity,
            locationQuery,
            floor,
            building,
            status,
            requiredFeatures,
        )

        return meetingRoomQueryUseCase.findAvailableRooms(
            slot,
            criteria,
            UserId.of(user.id),
        ).map { it.toResponseDto() }
    }

    @GetMapping("/{roomId}/available-slots")
    @Operation(
        summary = "Получение доступных временных слотов по переговорке",
        description = "Получение доступных временных слотов для переговорки на определенную дату",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Успех"),
        ApiResponse(responseCode = "401", description = "Не авторизован", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Переговорка не найдена", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = [Content()]),
    )
    fun findAvailableSlots(
        @AuthenticationPrincipal user: User,
        @PathVariable roomId: UUID,
        @RequestParam durationMinutes: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): List<TimeSlotDto> {
        val slots = meetingRoomQueryUseCase.findAvailableSlots(
            roomId = MeetingRoomId.of(roomId.toString()),
            date = date,
            duration = Duration.ofMinutes(durationMinutes),
            userId = UserId.of(user.id),
        )
        return slots.map { TimeSlotDto.from(it) }
    }

    private fun buildCriteria(
        minCapacity: Int?,
        locationQuery: String?,
        floor: Int?,
        building: String?,
        status: String?,
        requiredFeatures: List<String>,
    ): MeetingRoomSearchCriteria {
        val features = requiredFeatures.mapNotNullTo(mutableSetOf()) { RoomFeature.fromString(it) }

        return MeetingRoomSearchCriteria(
            minCapacity = minCapacity,
            locationQuery = locationQuery,
            floor = floor,
            building = building,
            status = status?.let { MeetingRoom.MeetingRoomStatus.fromString(it) },
            requiredFeatures = features,
        )
    }
}
