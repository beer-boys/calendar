package ru.itmo.dws.calendar

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component

@Component
@EnableScheduling
@Order(Ordered.LOWEST_PRECEDENCE)
class Scheduler
