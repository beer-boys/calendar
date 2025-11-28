package ru.itmo.dws.calendar.repository

import java.time.LocalDateTime
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import ru.itmo.dws.calendar.model.NotificationOutbox
import ru.itmo.dws.calendar.repository.extension.NotificationOutboxExtension

interface NotificationOutboxRepository : NotificationOutboxExtension, CrudRepository<NotificationOutbox, Long> {

    @Query(
        """
        UPDATE notification_outbox
        SET status = 'IN_PROGRESS'::notification_outbox_status,
            updated_at = :now
        WHERE id IN (
            SELECT id
            FROM notification_outbox
            WHERE status = 'PENDING'::notification_outbox_status
              AND next_retry_at <= :now
            ORDER BY next_retry_at ASC, id ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
        )
        RETURNING *
    """
    )
    fun pollTasks(
        @Param("batchSize") batchSize: Int,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): List<NotificationOutbox>

    @Query(
        """
        UPDATE notification_outbox
        SET status = 'PENDING'::notification_outbox_status,
            updated_at = :now,
            next_retry_at = :now
        WHERE status = 'IN_PROGRESS'::notification_outbox_status
          AND updated_at <= :stuckThreshold
        RETURNING *
    """
    )
    fun rescueStuckTasks(
        @Param("stuckThreshold") stuckThreshold: LocalDateTime,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): List<NotificationOutbox>
}
