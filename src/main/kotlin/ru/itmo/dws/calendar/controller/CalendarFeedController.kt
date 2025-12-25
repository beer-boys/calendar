package ru.itmo.dws.calendar.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.ZonedDateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.itmo.dws.calendar.configuration.BasePath
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.input.CalendarFeedUseCase
import ru.itmo.dws.calendar.dto.feed.CalendarFeedResponseDto
import ru.itmo.dws.calendar.model.User

@RestController
@RequestMapping("${BasePath.BASE}/calendar/feed")
@Tag(name = "Calendar Feed", description = "API для получения обогащённой ленты событий календаря")
class CalendarFeedController(
    private val calendarFeedUseCase: CalendarFeedUseCase
) {

    @GetMapping
    @Operation(
        summary = "Получить ленту событий календаря",
        description = """
            Возвращает объединённый список событий за указанный период:
            - События из внешнего календаря (Google Calendar)
            - Экземпляры привычек
            - Блоки фокус-времени
            - Встречи
            
            Для зеркалированных сущностей (имеющих представление во внешнем календаре) 
            возвращается одна объединённая запись с обогащёнными данными.
        """
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список событий"),
        ApiResponse(responseCode = "400", description = "Некорректный диапазон дат", content = [Content()]),
        ApiResponse(responseCode = "401", description = "Не авторизован", content = [Content()])
    )
    fun getCalendarFeed(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "Начало периода (ISO 8601)", example = "2025-01-01T00:00:00+03:00")
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        start: ZonedDateTime,
        @Parameter(description = "Конец периода (ISO 8601)", example = "2025-01-31T23:59:59+03:00")
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        end: ZonedDateTime
    ): ResponseEntity<CalendarFeedResponseDto> {
        require(start.isBefore(end)) { "Start date must be before end date" }

        val userId = UserId(user.id)
        val timeRange = TimeSlot(start, end)

        val result = calendarFeedUseCase.getCalendarFeed(userId, timeRange)

        return ResponseEntity.ok(CalendarFeedResponseDto.fromDomain(result))
    }
}
