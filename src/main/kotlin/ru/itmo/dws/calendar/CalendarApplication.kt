package ru.itmo.dws.calendar

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CalendarApplication

@SuppressWarnings("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<CalendarApplication>(*args)
}
