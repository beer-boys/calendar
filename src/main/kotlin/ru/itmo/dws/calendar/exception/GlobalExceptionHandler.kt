package ru.itmo.dws.calendar.exception

import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.itmo.dws.calendar.core.domain.exception.CalendarDomainException
import ru.itmo.dws.calendar.core.domain.exception.ExternalCalendarSyncException
import ru.itmo.dws.calendar.core.domain.exception.HabitNotFoundException
import ru.itmo.dws.calendar.core.domain.exception.HabitValidationException

@RestControllerAdvice
class GlobalExceptionHandler {

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(HabitNotFoundException::class)
    fun handleHabitNotFound(ex: HabitNotFoundException): ResponseEntity<ErrorResponse> {
        log.warn("Habit not found: ${ex.habitId}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    status = HttpStatus.NOT_FOUND.value(),
                    error = "Not Found",
                    message = ex.message ?: "Habit not found",
                    timestamp = Instant.now()
                )
            )
    }

    @ExceptionHandler(HabitValidationException::class)
    fun handleHabitValidation(ex: HabitValidationException): ResponseEntity<ErrorResponse> {
        log.warn("Habit validation error: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Validation Error",
                    message = ex.message ?: "Validation error",
                    field = ex.field,
                    timestamp = Instant.now()
                )
            )
    }

    @ExceptionHandler(ExternalCalendarSyncException::class)
    fun handleExternalCalendarSync(ex: ExternalCalendarSyncException): ResponseEntity<ErrorResponse> {
        log.error("External calendar sync error", ex)
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_GATEWAY.value(),
                    error = "External Service Error",
                    message = ex.message ?: "Failed to sync with external calendar",
                    timestamp = Instant.now()
                )
            )
    }

    @ExceptionHandler(CalendarDomainException::class)
    fun handleCalendarDomain(ex: CalendarDomainException): ResponseEntity<ErrorResponse> {
        log.warn("Calendar domain error: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Domain Error",
                    message = ex.message ?: "Domain error",
                    timestamp = Instant.now()
                )
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.warn("Illegal argument: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Bad Request",
                    message = ex.message ?: "Invalid request",
                    timestamp = Instant.now()
                )
            )
    }
}

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val field: String? = null,
    val timestamp: Instant
)
