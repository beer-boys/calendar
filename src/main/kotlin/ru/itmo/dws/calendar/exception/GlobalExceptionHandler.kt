package ru.itmo.dws.calendar.exception

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import ru.itmo.dws.calendar.core.domain.exception.CalendarDomainException
import ru.itmo.dws.calendar.core.domain.exception.ExternalCalendarSyncException
import ru.itmo.dws.calendar.core.domain.exception.HabitNotFoundException
import ru.itmo.dws.calendar.core.domain.exception.HabitValidationException

@RestControllerAdvice
@Suppress("TooManyFunctions")
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
        log.warn("Illegal argument: ${ex.message}", ex)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Bad Request",
                    message = ex.message ?: "Invalid request parameter",
                    timestamp = Instant.now()
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        log.warn("Validation failed: ${ex.bindingResult.errorCount} errors")

        val errors = ex.bindingResult.allErrors.map { error ->
            val fieldName = (error as? FieldError)?.field ?: "unknown"
            val message = error.defaultMessage ?: "Validation failed"
            ValidationError(field = fieldName, message = message)
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ValidationErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Validation Failed",
                    message = "Request validation failed",
                    errors = errors,
                    timestamp = Instant.now()
                )
            )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        log.warn("Malformed JSON request: ${ex.message}", ex)

        val cause = ex.cause
        val message = when (cause) {
            is MissingKotlinParameterException -> {
                "Missing required field: ${cause.parameter.name}"
            }
            else -> {
                "Malformed JSON request: ${ex.mostSpecificCause.message}"
            }
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Bad Request",
                    message = message,
                    timestamp = Instant.now()
                )
            )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        log.warn("Type mismatch for parameter '${ex.name}': ${ex.message}")

        val message = "Invalid value for parameter '${ex.name}': expected ${ex.requiredType?.simpleName}"

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Bad Request",
                    message = message,
                    field = ex.name,
                    timestamp = Instant.now()
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error occurred", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    error = "Internal Server Error",
                    message = "An unexpected error occurred. Please try again later.",
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

data class ValidationErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val errors: List<ValidationError>,
    val timestamp: Instant
)

data class ValidationError(
    val field: String,
    val message: String
)
