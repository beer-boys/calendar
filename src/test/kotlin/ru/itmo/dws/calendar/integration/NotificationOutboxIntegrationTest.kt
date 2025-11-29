package ru.itmo.dws.calendar.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.itmo.dws.calendar.Scheduler
import ru.itmo.dws.calendar.model.NotificationOutbox
import ru.itmo.dws.calendar.model.NotificationOutboxStatus
import ru.itmo.dws.calendar.model.NotificationOutboxType
import ru.itmo.dws.calendar.repository.NotificationOutboxRepository
import ru.itmo.dws.calendar.service.notification.email.EmailService
import ru.itmo.dws.calendar.service.notification.model.EmailNotificationPayload
import ru.itmo.dws.calendar.service.notification.model.NotificationPayload

class NotificationOutboxIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var outboxRepository: NotificationOutboxRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var scheduler: Scheduler

    @MockkBean
    private lateinit var emailService: EmailService

    @BeforeEach
    fun setup() {
        outboxRepository.deleteAll()
        every { emailService.sendEmail(any(), any(), any()) } just runs
    }

    @AfterEach
    fun down() {
        clearAllMocks()
    }

    @Test
    fun `should process pending task successfully`() {
        val payload = EmailNotificationPayload(
            to = "test@example.com",
            subject = "Hello",
            body = "World"
        )

        val task = createOutboxTask(payload, NotificationOutboxStatus.PENDING)

        await.atMost(3, TimeUnit.SECONDS)
            .untilAsserted {
                val updatedTask = outboxRepository.findById(task.id!!).orElseThrow()

                assertThat(updatedTask.status)
                    .withFailMessage { "Expected task: $updatedTask to be in DONE status, but was ${updatedTask.status}" }
                    .isEqualTo(NotificationOutboxStatus.DONE)
                assertThat(updatedTask.updatedAt).isAfter(task.updatedAt)
            }

        verify(atLeast = 1) {
            emailService.sendEmail("test@example.com", "Hello", "World")
        }
    }

    @Test
    fun `should retry failed task`() {
        every { emailService.sendEmail(any(), any(), any()) } throws RuntimeException("SMTP Timeout")

        val payload = EmailNotificationPayload(to = "retry@test.com", subject = "Err", body = "Body")
        val task = createOutboxTask(payload, NotificationOutboxStatus.PENDING)

        await.atMost(2, TimeUnit.SECONDS).untilAsserted {
            val updatedTask = outboxRepository.findById(task.id!!).orElseThrow()

            assertThat(updatedTask.attemptsCount).isEqualTo(1)
            assertThat(updatedTask.nextRetryAt).isAfter(LocalDateTime.now())
            assertThat(updatedTask.status).isEqualTo(NotificationOutboxStatus.PENDING)
        }
    }

    @Test
    fun `should fail task after max retries`() {
        every { emailService.sendEmail(any(), any(), any()) } throws RuntimeException("Fatal Error")

        val payload = EmailNotificationPayload(to = "fail@test.com", subject = "Bye", body = "Body")

        val entity = NotificationOutbox(
            payload = objectMapper.writeValueAsString(payload),
            type = NotificationOutboxType.EMAIL,
            status = NotificationOutboxStatus.PENDING,
            attemptsCount = 4,
            nextRetryAt = LocalDateTime.now().minusSeconds(1)
        )
        val taskId = outboxRepository.insert(entity)

        await.atMost(2, TimeUnit.SECONDS).untilAsserted {
            val updatedTask = outboxRepository.findById(taskId).orElseThrow()

            assertThat(updatedTask.attemptsCount).isEqualTo(5)
            assertThat(updatedTask.status).isEqualTo(NotificationOutboxStatus.FAILED)
        }
    }

    @Test
    fun `should rescue stuck IN_PROGRESS tasks`() {
        val payload = EmailNotificationPayload(to = "zombie@test.com", subject = "Zombie", body = "Brain")
        val oldDate = LocalDateTime.now().minusMinutes(20)

        val entity = NotificationOutbox(
            payload = objectMapper.writeValueAsString(payload),
            type = NotificationOutboxType.EMAIL,
            status = NotificationOutboxStatus.IN_PROGRESS,
            updatedAt = oldDate,
            nextRetryAt = oldDate
        )
        val taskId = outboxRepository.insert(entity)

        scheduler.rescueNotificationOutbox()

        val rescuedTask = outboxRepository.findById(taskId).orElseThrow()

        assertThat(rescuedTask.nextRetryAt).isAfter(oldDate)
        assertThat(rescuedTask.nextRetryAt).isBefore(LocalDateTime.now())
        assertThat(rescuedTask.status).isEqualTo(NotificationOutboxStatus.PENDING)
    }

    private fun createOutboxTask(
        payload: NotificationPayload,
        status: NotificationOutboxStatus,
    ): NotificationOutbox {
        val entity = NotificationOutbox(
            payload = objectMapper.writeValueAsString(payload),
            type = NotificationOutboxType.EMAIL,
            status = status,
            nextRetryAt = LocalDateTime.now().minusSeconds(1)
        )
        val id = outboxRepository.insert(entity)
        return outboxRepository.findById(id).orElseThrow()
    }
}
