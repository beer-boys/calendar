package ru.itmo.dws.calendar.service.notification

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import kotlin.math.pow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import ru.itmo.dws.calendar.model.NotificationOutbox
import ru.itmo.dws.calendar.model.NotificationOutboxStatus
import ru.itmo.dws.calendar.model.NotificationOutboxStatus.FAILED
import ru.itmo.dws.calendar.model.NotificationOutboxStatus.PENDING
import ru.itmo.dws.calendar.model.NotificationOutboxType.EMAIL
import ru.itmo.dws.calendar.repository.NotificationOutboxRepository
import ru.itmo.dws.calendar.service.notification.email.EmailService
import ru.itmo.dws.calendar.service.notification.model.EmailNotificationPayload
import ru.itmo.dws.calendar.service.notification.model.NotificationPayload

@Service
class NotificationOutboxService(
    private val emailService: EmailService,
    private val objectMapper: ObjectMapper,
    private val transactionTemplate: TransactionTemplate,
    private val outboxRepository: NotificationOutboxRepository,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val BATCH_SIZE = 50
        private const val BACKOFF_BASE = 2.0
        private const val MAX_RETRIES = 5
        private const val STUCK_THRESHOLD_MINUTES = 10L
    }

    @Transactional
    fun schedule(payload: NotificationPayload) {
        val json = objectMapper.writeValueAsString(payload)

        val entity = NotificationOutbox(
            payload = json,
            type = EMAIL,
        )

        outboxRepository.save(entity)
    }

    fun processPendingTasks() {
        val tasks = outboxRepository.pollTasks(batchSize = BATCH_SIZE)

        tasks.forEach { task ->
            processSingleTask(task)
        }
    }

    fun rescueStuckTasks() {
        val stuckThreshold = LocalDateTime.now().minusMinutes(STUCK_THRESHOLD_MINUTES)

        val rescued = transactionTemplate.execute {
            outboxRepository.rescueStuckTasks(stuckThreshold)
        } ?: emptyList()

        if (rescued.isNotEmpty()) {
            logger.warn("Rescued {} stuck tasks: {}", rescued.size, rescued.map { it.id })
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processSingleTask(task: NotificationOutbox) {
        try {
            sendToNotificationProvider(task)

            transactionTemplate.executeWithoutResult {
                val doneTask = task.copy(
                    status = NotificationOutboxStatus.DONE,
                    updatedAt = LocalDateTime.now(),
                )

                outboxRepository.save(doneTask)
            }
        } catch (e: Exception) {
            logger.error("Error processing task: ${task.id}", e)
            val nextTry = calculateNextRetry(task.attemptsCount)

            transactionTemplate.executeWithoutResult {
                val failedTask = task.copy(
                    status = if (task.attemptsCount >= MAX_RETRIES) FAILED else PENDING,
                    nextRetryAt = nextTry,
                    attemptsCount = task.attemptsCount + 1,
                    updatedAt = LocalDateTime.now(),
                )

                outboxRepository.save(failedTask)
            }
        }
    }

    private fun sendToNotificationProvider(task: NotificationOutbox) {
        when (task.type) {
            EMAIL -> sendToEmailProvider(task.payload)
        }
    }

    private fun sendToEmailProvider(payload: String) {
        val emailPayload = objectMapper.readValue(payload, EmailNotificationPayload::class.java)

        emailService.sendEmail(
            to = emailPayload.to,
            subject = emailPayload.subject,
            body = emailPayload.body,
        )
    }

    private fun calculateNextRetry(attempts: Int): LocalDateTime {
        val minutes = BACKOFF_BASE.pow(attempts).toLong()
        return LocalDateTime.now().plusMinutes(minutes)
    }
}
