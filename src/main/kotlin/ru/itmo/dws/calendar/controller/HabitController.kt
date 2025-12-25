package ru.itmo.dws.calendar.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.time.LocalDate
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.itmo.dws.calendar.configuration.BasePath
import ru.itmo.dws.calendar.core.domain.exception.HabitNotFoundException
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.input.HabitManagementUseCase
import ru.itmo.dws.calendar.dto.habit.CreateHabitRequestDto
import ru.itmo.dws.calendar.dto.habit.HabitCreationResultDto
import ru.itmo.dws.calendar.dto.habit.HabitOccurrenceDto
import ru.itmo.dws.calendar.dto.habit.HabitResponseDto
import ru.itmo.dws.calendar.dto.habit.HabitSchedulePlanDto
import ru.itmo.dws.calendar.dto.habit.HabitSyncResultDto
import ru.itmo.dws.calendar.dto.habit.ScheduleHabitRequestDto
import ru.itmo.dws.calendar.dto.habit.UpdateHabitRequestDto
import ru.itmo.dws.calendar.model.User

@RestController
@RequestMapping("${BasePath.BASE}/habits")
@Tag(name = "Habits", description = "API для управления привычками пользователя")
@Suppress("TooManyFunctions")
class HabitController(
    private val habitManagementUseCase: HabitManagementUseCase
) {

    @PostMapping
    @Operation(
        summary = "Создать привычку",
        description = "Создаёт новую привычку и планирует её occurrences на ближайшие недели"
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Привычка успешно создана"),
        ApiResponse(responseCode = "400", description = "Некорректные данные запроса", content = [Content()]),
        ApiResponse(responseCode = "401", description = "Не авторизован", content = [Content()])
    )
    fun createHabit(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: CreateHabitRequestDto
    ): ResponseEntity<HabitCreationResultDto> {
        val userId = UserId(user.id)
        val domainRequest = request.toDomain(userId)
        val result = habitManagementUseCase.createHabit(domainRequest)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(HabitCreationResultDto.fromDomain(result))
    }

    @GetMapping
    @Operation(
        summary = "Получить список привычек",
        description = "Возвращает все привычки пользователя или привычки на определённую дату"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список привычек"),
        ApiResponse(responseCode = "401", description = "Не авторизован", content = [Content()])
    )
    fun getHabits(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "Фильтр по дате (опционально)")
        @RequestParam(required = false) date: LocalDate?
    ): ResponseEntity<List<HabitResponseDto>> {
        val userId = UserId(user.id)

        val habits = if (date != null) {
            habitManagementUseCase.getHabitsForDate(userId, date)
        } else {
            habitManagementUseCase.getHabits(userId)
        }

        return ResponseEntity.ok(habits.map { HabitResponseDto.fromDomain(it) })
    }

    @GetMapping("/{habitId}")
    @Operation(summary = "Получить привычку по ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Привычка найдена"),
        ApiResponse(responseCode = "403", description = "Доступ запрещён", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Привычка не найдена", content = [Content()])
    )
    fun getHabit(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "ID привычки") @PathVariable habitId: UUID
    ): ResponseEntity<HabitResponseDto> {
        val userId = UserId(user.id)
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        return ResponseEntity.ok(HabitResponseDto.fromDomain(habit))
    }

    @GetMapping("/{habitId}/plan")
    @Operation(
        summary = "Получить план расписания привычки",
        description = "Возвращает план occurrences привычки на указанное количество недель"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "План расписания"),
        ApiResponse(responseCode = "403", description = "Доступ запрещён", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Привычка не найдена", content = [Content()])
    )
    fun getHabitSchedulePlan(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "ID привычки") @PathVariable habitId: UUID,
        @Parameter(description = "Количество недель для планирования") @RequestParam(defaultValue = "4") weeks: Int
    ): ResponseEntity<HabitSchedulePlanDto> {
        val userId = UserId(user.id)
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val plan = habitManagementUseCase.planHabitSchedule(HabitId(habitId), weeks)
        return ResponseEntity.ok(HabitSchedulePlanDto.fromDomain(plan))
    }

    @PutMapping("/{habitId}")
    @Operation(summary = "Обновить привычку")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Привычка обновлена"),
        ApiResponse(responseCode = "400", description = "Некорректные данные", content = [Content()]),
        ApiResponse(responseCode = "403", description = "Доступ запрещён", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Привычка не найдена", content = [Content()])
    )
    fun updateHabit(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "ID привычки") @PathVariable habitId: UUID,
        @Valid @RequestBody request: UpdateHabitRequestDto
    ): ResponseEntity<HabitResponseDto> {
        val userId = UserId(user.id)
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val updatedHabit = habitManagementUseCase.updateHabit(
            HabitId(habitId),
            request.toDomain()
        )

        return ResponseEntity.ok(HabitResponseDto.fromDomain(updatedHabit))
    }

    @DeleteMapping("/{habitId}")
    @Operation(
        summary = "Удалить привычку",
        description = "Удаляет привычку и все её occurrences из внешнего календаря"
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Привычка удалена"),
        ApiResponse(responseCode = "403", description = "Доступ запрещён", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Привычка не найдена", content = [Content()])
    )
    fun deleteHabit(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "ID привычки") @PathVariable habitId: UUID
    ): ResponseEntity<Unit> {
        val userId = UserId(user.id)
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        habitManagementUseCase.deleteHabit(HabitId(habitId))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{habitId}/schedule")
    @Operation(
        summary = "Назначить слот для привычки",
        description = "Назначает конкретный временной слот для привычки на определённую дату"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Слот назначен"),
        ApiResponse(responseCode = "400", description = "Слот вне окна гибкости", content = [Content()]),
        ApiResponse(responseCode = "403", description = "Доступ запрещён", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Привычка не найдена", content = [Content()])
    )
    fun scheduleHabit(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "ID привычки") @PathVariable habitId: UUID,
        @Valid @RequestBody request: ScheduleHabitRequestDto
    ): ResponseEntity<HabitResponseDto> {
        val userId = UserId(user.id)
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val timeSlot = TimeSlot(start = request.startTime, end = request.endTime)
        val scheduledHabit = habitManagementUseCase.scheduleHabitForDate(
            HabitId(habitId),
            request.date,
            timeSlot
        )

        return ResponseEntity.ok(HabitResponseDto.fromDomain(scheduledHabit))
    }

    @DeleteMapping("/{habitId}/schedule")
    @Operation(
        summary = "Очистить расписание привычки",
        description = "Удаляет назначенный временной слот привычки на указанную дату"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Расписание очищено"),
        ApiResponse(responseCode = "403", description = "Доступ запрещён", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Привычка не найдена", content = [Content()])
    )
    fun clearHabitSchedule(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "ID привычки") @PathVariable habitId: UUID,
        @Parameter(description = "Дата для очистки") @RequestParam date: LocalDate
    ): ResponseEntity<HabitResponseDto> {
        val userId = UserId(user.id)
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val clearedHabit = habitManagementUseCase.clearHabitScheduleForDate(
            HabitId(habitId),
            date
        )

        return ResponseEntity.ok(HabitResponseDto.fromDomain(clearedHabit))
    }

    @PostMapping("/{habitId}/sync")
    @Operation(
        summary = "Синхронизировать с внешним календарём",
        description = "Синхронизирует occurrences привычки с Google Calendar на указанное количество недель"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Синхронизация выполнена"),
        ApiResponse(responseCode = "403", description = "Доступ запрещён", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Привычка не найдена", content = [Content()])
    )
    fun syncHabitToExternalCalendar(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "ID привычки") @PathVariable habitId: UUID,
        @Parameter(description = "Количество недель для синхронизации") @RequestParam(defaultValue = "4") weeks: Int
    ): ResponseEntity<HabitSyncResultDto> {
        val userId = UserId(user.id)
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val result = habitManagementUseCase.syncHabitToExternalCalendar(HabitId(habitId), weeks)
        return ResponseEntity.ok(HabitSyncResultDto.fromDomain(result))
    }

    @GetMapping("/{habitId}/occurrences")
    @Operation(
        summary = "Получить occurrences привычки",
        description = "Возвращает список всех occurrences привычки или за указанный период"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список occurrences"),
        ApiResponse(responseCode = "403", description = "Доступ запрещён", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Привычка не найдена", content = [Content()])
    )
    fun getHabitOccurrences(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "ID привычки") @PathVariable habitId: UUID,
        @Parameter(description = "Начало периода (опционально)") @RequestParam(required = false) startDate: LocalDate?,
        @Parameter(description = "Конец периода (опционально)") @RequestParam(required = false) endDate: LocalDate?
    ): ResponseEntity<List<HabitOccurrenceDto>> {
        val userId = UserId(user.id)
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val occurrences = if (startDate != null && endDate != null) {
            habitManagementUseCase.getHabitOccurrences(HabitId(habitId), startDate, endDate)
        } else {
            habitManagementUseCase.getHabitOccurrences(HabitId(habitId))
        }

        return ResponseEntity.ok(occurrences.map { HabitOccurrenceDto.fromDomain(it) })
    }
}
