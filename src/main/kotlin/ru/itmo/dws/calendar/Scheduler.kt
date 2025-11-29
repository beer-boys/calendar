package ru.itmo.dws.calendar

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.service.notification.NotificationOutboxService

@Component
@EnableScheduling
@Order(Ordered.LOWEST_PRECEDENCE)
class Scheduler(private val notificationOutboxService: NotificationOutboxService) {

    /**
     * Main notification outbox worker.
     *
     * Due to skip lock and transaction controller, this method is safe to run on multiple instances.
     */
    @Scheduled(fixedDelayString = "\${outbox.pollInterval}")
    fun processNotificationOutbox() {
        notificationOutboxService.processPendingTasks()
    }

    /**
     * Rescue notification outbox worker.
     *
     * Finds tasks that are stuck in IN_PROGRESS status and resets them to PENDING.
     */
    @Scheduled(fixedDelayString = "\${outbox.rescueInterval}")
    fun rescueNotificationOutbox() {
        notificationOutboxService.rescueStuckTasks()
    }
}
