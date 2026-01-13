package ru.itmo.dws.calendar.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.itmo.dws.calendar.configuration.BasePath
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomBookingId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId
import ru.itmo.dws.calendar.core.port.input.room.BookMeetingRoomCommand
import ru.itmo.dws.calendar.core.port.input.room.CancelMeetingRoomBookingCommand
import ru.itmo.dws.calendar.core.port.input.room.MeetingRoomBookingUseCase
import ru.itmo.dws.calendar.dto.room.booking.RoomBookingCreateDto
import ru.itmo.dws.calendar.dto.room.booking.RoomBookingResponseDto
import ru.itmo.dws.calendar.mapper.toResponseDto
import ru.itmo.dws.calendar.model.User

@RestController
@RequestMapping("${BasePath.BASE}/meeting-room-bookings")
@Tag(name = "Meeting Rooms", description = "API для работы с бронированием переговорок")
@Suppress("TooManyFunctions")
class MeetingRoomBookingController(private val bookingUseCase: MeetingRoomBookingUseCase) {

    @GetMapping
    @Operation(
        summary = "Получение всех бронирований пользователя",
        description = "Получение всех бронирований пользователя",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Успех"),
        ApiResponse(responseCode = "401", description = "Не авторизован", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = [Content()]),
    )
    fun getBookings(
        @AuthenticationPrincipal user: User,
    ): List<RoomBookingResponseDto> {
        return bookingUseCase.getUserBookings(UserId.of(user.id)).map { it.toResponseDto() }
    }

    @GetMapping("/{bookingId}")
    @Operation(
        summary = "Получение бронирования",
        description = "Получение бронирования переговорки",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Успех"),
        ApiResponse(responseCode = "401", description = "Не авторизован", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Бронирование не найдено", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = [Content()]),
    )
    fun getBooking(
        @AuthenticationPrincipal user: User,
        @PathVariable bookingId: UUID,
    ): RoomBookingResponseDto {
        val booking = bookingUseCase.getBooking(
            bookingId = MeetingRoomBookingId(bookingId),
            userId = UserId.of(user.id),
        )

        return booking.toResponseDto()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Бронирование переговорки",
        description = "Бронирование переговорки на заданный период",
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Бронирование создано"),
        ApiResponse(responseCode = "401", description = "Не авторизован", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Переговорка не найдена", content = [Content()]),
        ApiResponse(
            responseCode = "409",
            description = "Бронирование невозможно из-за пересечения с другими бронированиями",
            content = [Content()],
        ),
        ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = [Content()]),
    )
    fun bookRoom(
        @AuthenticationPrincipal user: User,
        @RequestBody request: RoomBookingCreateDto,
    ): RoomBookingResponseDto {
        val command = BookMeetingRoomCommand(
            roomId = MeetingRoomId.of(request.roomId),
            timeSlot = TimeSlot(request.timeSlot.start, request.timeSlot.end),
            purpose = request.purpose,
            sourceMeetingId = request.sourceMeetingId,
            organizerId = UserId.of(user.id),
        )

        return bookingUseCase.bookRoom(command).toResponseDto()
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(
        summary = "Отмена бронирования переговорки",
        description = "Бронирование переговорки на заданный период",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Бронирование отменено"),
        ApiResponse(responseCode = "401", description = "Не авторизован", content = [Content()]),
        ApiResponse(responseCode = "403", description = "Нет доступа к бронированию", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Бронирование не найдено", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = [Content()]),
    )
    fun cancelBooking(
        @AuthenticationPrincipal user: User,
        @PathVariable bookingId: UUID,
    ) {
        val command = CancelMeetingRoomBookingCommand(
            bookingId = MeetingRoomBookingId(bookingId),
            cancelledBy = UserId.of(user.id),
        )

        return bookingUseCase.cancelBooking(command)
    }
}
