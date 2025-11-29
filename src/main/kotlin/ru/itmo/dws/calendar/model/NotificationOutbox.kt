package ru.itmo.dws.calendar.model

import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("notification_outbox")
data class NotificationOutbox(
    @Id
    val id: Long? = null,
    val status: NotificationOutboxStatus = NotificationOutboxStatus.PENDING,
    val type: NotificationOutboxType,
    val payload: String,
    val attemptsCount: Int = 0,
    val nextRetryAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

enum class NotificationOutboxStatus {
    PENDING, IN_PROGRESS, DONE, FAILED
}

enum class NotificationOutboxType {
    EMAIL,
}
