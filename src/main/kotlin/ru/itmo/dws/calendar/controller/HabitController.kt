package ru.itmo.dws.calendar.controller

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
import ru.itmo.dws.calendar.dto.habit.HabitResponseDto
import ru.itmo.dws.calendar.dto.habit.ScheduleHabitRequestDto
import ru.itmo.dws.calendar.dto.habit.UpdateHabitRequestDto
import ru.itmo.dws.calendar.model.User

@RestController
@RequestMapping("${BasePath.BASE}/habits")
class HabitController(
    private val habitManagementUseCase: HabitManagementUseCase
) {

    @PostMapping
    fun createHabit(
        @AuthenticationPrincipal user: User,
        @RequestBody request: CreateHabitRequestDto
    ): ResponseEntity<HabitCreationResultDto> {
        val userId = UserId(user.id)
        val domainRequest = request.toDomain(userId)
        val result = habitManagementUseCase.createHabit(domainRequest)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(HabitCreationResultDto.fromDomain(result))
    }

    @GetMapping
    fun getHabits(
        @AuthenticationPrincipal user: User,
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
    fun getHabit(
        @AuthenticationPrincipal user: User,
        @PathVariable habitId: UUID
    ): ResponseEntity<HabitResponseDto> {
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId.value != user.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        return ResponseEntity.ok(HabitResponseDto.fromDomain(habit))
    }

    @PutMapping("/{habitId}")
    fun updateHabit(
        @AuthenticationPrincipal user: User,
        @PathVariable habitId: UUID,
        @RequestBody request: UpdateHabitRequestDto
    ): ResponseEntity<HabitResponseDto> {
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId.value != user.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val updatedHabit = habitManagementUseCase.updateHabit(
            HabitId(habitId),
            request.toDomain()
        )

        return ResponseEntity.ok(HabitResponseDto.fromDomain(updatedHabit))
    }

    @DeleteMapping("/{habitId}")
    fun deleteHabit(
        @AuthenticationPrincipal user: User,
        @PathVariable habitId: UUID
    ): ResponseEntity<Unit> {
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId.value != user.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        habitManagementUseCase.deleteHabit(HabitId(habitId))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{habitId}/schedule")
    fun scheduleHabit(
        @AuthenticationPrincipal user: User,
        @PathVariable habitId: UUID,
        @RequestBody request: ScheduleHabitRequestDto
    ): ResponseEntity<HabitResponseDto> {
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId.value != user.id) {
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
    fun clearHabitSchedule(
        @AuthenticationPrincipal user: User,
        @PathVariable habitId: UUID,
        @RequestParam date: LocalDate
    ): ResponseEntity<HabitResponseDto> {
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId.value != user.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val clearedHabit = habitManagementUseCase.clearHabitScheduleForDate(
            HabitId(habitId),
            date
        )

        return ResponseEntity.ok(HabitResponseDto.fromDomain(clearedHabit))
    }
}
