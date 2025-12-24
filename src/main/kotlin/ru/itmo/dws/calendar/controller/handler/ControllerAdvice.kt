package ru.itmo.dws.calendar.controller.handler

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import ru.itmo.dws.calendar.core.domain.exception.BookingNotFound
import ru.itmo.dws.calendar.core.domain.exception.CalendarDomainException
import ru.itmo.dws.calendar.core.domain.exception.MeetingRoomInactive
import ru.itmo.dws.calendar.core.domain.exception.MeetingRoomNotFound
import ru.itmo.dws.calendar.core.domain.exception.TimeSlotNotAvailable
import ru.itmo.dws.calendar.exception.CalendarException

@RestControllerAdvice
class ControllerAdvice : ResponseEntityExceptionHandler() {

    @ExceptionHandler(CalendarException::class)
    fun handleCaseModuleException(ex: CalendarException): ProblemDetail {
        val detail = ProblemDetail.forStatusAndDetail(ex.statusCode, ex.message)
        logger.error(ex.message ?: "Something went wrong", ex)
        return detail
    }

    @ExceptionHandler(CalendarDomainException::class)
    fun handleDomainException(ex: CalendarDomainException): ProblemDetail {
        val code = when (ex) {
            is BookingNotFound -> HttpStatus.NOT_FOUND
            is MeetingRoomNotFound -> HttpStatus.NOT_FOUND
            is TimeSlotNotAvailable -> HttpStatus.CONFLICT
            is MeetingRoomInactive -> HttpStatus.CONFLICT
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        if (code == HttpStatus.INTERNAL_SERVER_ERROR) {
            logger.error(ex.message ?: "Something went wrong", ex)
        } else {
            logger.info(ex.message ?: "Something went wrong", ex)
        }

        val detail = ProblemDetail.forStatusAndDetail(code, ex.message)
        return detail
    }

    @ExceptionHandler(Exception::class)
    fun handleServerException(ex: Exception): ProblemDetail {
        val msg = "Something went wrong"
        val detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, msg)
        logger.error(msg, ex)
        return detail
    }
}
