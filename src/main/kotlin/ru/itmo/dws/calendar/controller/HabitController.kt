package ru.itmo.dws.calendar.controller

import java.time.LocalDate
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
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

@RestController
@RequestMapping("${BasePath.BASE}/habits")
@Suppress("TooManyFunctions")
class HabitController(
    private val habitManagementUseCase: HabitManagementUseCase
) {

    @PostMapping
    fun createHabit(
        authentication: OAuth2AuthenticationToken,
        @RequestBody request: CreateHabitRequestDto
    ): ResponseEntity<HabitCreationResultDto> {
        val userId = extractUserId(authentication)
        val domainRequest = request.toDomain(userId)
        val result = habitManagementUseCase.createHabit(domainRequest)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(HabitCreationResultDto.fromDomain(result))
    }

    @GetMapping
    fun getHabits(
        authentication: OAuth2AuthenticationToken,
        @RequestParam(required = false) date: LocalDate?
    ): ResponseEntity<List<HabitResponseDto>> {
        val userId = extractUserId(authentication)

        val habits = if (date != null) {
            habitManagementUseCase.getHabitsForDate(userId, date)
        } else {
            habitManagementUseCase.getHabits(userId)
        }

        return ResponseEntity.ok(habits.map { HabitResponseDto.fromDomain(it) })
    }

    @GetMapping("/{habitId}")
    fun getHabit(
        authentication: OAuth2AuthenticationToken,
        @PathVariable habitId: UUID
    ): ResponseEntity<HabitResponseDto> {
        val userId = extractUserId(authentication)
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        return ResponseEntity.ok(HabitResponseDto.fromDomain(habit))
    }

    @GetMapping("/{habitId}/plan")
    fun getHabitSchedulePlan(
        authentication: OAuth2AuthenticationToken,
        @PathVariable habitId: UUID,
        @RequestParam(defaultValue = "4") weeks: Int
    ): ResponseEntity<HabitSchedulePlanDto> {
        val userId = extractUserId(authentication)
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val plan = habitManagementUseCase.planHabitSchedule(HabitId(habitId), weeks)
        return ResponseEntity.ok(HabitSchedulePlanDto.fromDomain(plan))
    }

    @PutMapping("/{habitId}")
    fun updateHabit(
        authentication: OAuth2AuthenticationToken,
        @PathVariable habitId: UUID,
        @RequestBody request: UpdateHabitRequestDto
    ): ResponseEntity<HabitResponseDto> {
        val userId = extractUserId(authentication)
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
    fun deleteHabit(
        authentication: OAuth2AuthenticationToken,
        @PathVariable habitId: UUID
    ): ResponseEntity<Unit> {
        val userId = extractUserId(authentication)
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        habitManagementUseCase.deleteHabit(HabitId(habitId))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{habitId}/schedule")
    fun scheduleHabit(
        authentication: OAuth2AuthenticationToken,
        @PathVariable habitId: UUID,
        @RequestBody request: ScheduleHabitRequestDto
    ): ResponseEntity<HabitResponseDto> {
        val userId = extractUserId(authentication)
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
    fun clearHabitSchedule(
        authentication: OAuth2AuthenticationToken,
        @PathVariable habitId: UUID,
        @RequestParam date: LocalDate
    ): ResponseEntity<HabitResponseDto> {
        val userId = extractUserId(authentication)
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
    fun syncHabitToExternalCalendar(
        authentication: OAuth2AuthenticationToken,
        @PathVariable habitId: UUID,
        @RequestParam(defaultValue = "4") weeks: Int
    ): ResponseEntity<HabitSyncResultDto> {
        val userId = extractUserId(authentication)
        val habit = habitManagementUseCase.getHabit(HabitId(habitId))
            ?: throw HabitNotFoundException(HabitId(habitId))

        if (habit.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val result = habitManagementUseCase.syncHabitToExternalCalendar(HabitId(habitId), weeks)
        return ResponseEntity.ok(HabitSyncResultDto.fromDomain(result))
    }

    @GetMapping("/{habitId}/occurrences")
    fun getHabitOccurrences(
        authentication: OAuth2AuthenticationToken,
        @PathVariable habitId: UUID,
        @RequestParam(required = false) startDate: LocalDate?,
        @RequestParam(required = false) endDate: LocalDate?
    ): ResponseEntity<List<HabitOccurrenceDto>> {
        val userId = extractUserId(authentication)
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

    private fun extractUserId(authentication: OAuth2AuthenticationToken): UserId {
        val oAuth2User: OAuth2User = authentication.principal
        val email = oAuth2User.getAttribute<String>("email")
        checkNotNull(email) { "Email not found in OAuth2 token" }
        return UserId(UUID.nameUUIDFromBytes(email.toByteArray()))
    }
}
