package ru.itmo.dws.calendar.repository.extension

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.model.NotificationOutbox

@Component
@Suppress("UseCheckOrError")
class NotificationOutboxExtensionImpl(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) : NotificationOutboxExtension {

    companion object {
        private const val INSERT_SQL = """
            INSERT INTO notification_outbox (
                status,
                type,
                payload,
                attempts_count,
                next_retry_at,
                updated_at,
                created_at
            ) VALUES (
                :status::notification_outbox_status,
                :type::notification_outbox_type,
                :payload::jsonb,
                :attemptsCount,
                :nextRetryAt,
                :updatedAt,
                :createdAt
            )
            RETURNING id
        """

        private const val UPDATE_SQL = """
            UPDATE notification_outbox SET
                status = :status::notification_outbox_status,
                type = :type::notification_outbox_type,
                payload = :payload::jsonb,
                attempts_count = :attemptsCount,
                next_retry_at = :nextRetryAt,
                updated_at = :updatedAt
            WHERE id = :id
        """
    }

    override fun insert(notificationOutbox: NotificationOutbox): Long {
        val params = MapSqlParameterSource()
            .addValue("status", notificationOutbox.status.name)
            .addValue("type", notificationOutbox.type.name)
            .addValue("payload", notificationOutbox.payload)
            .addValue("attemptsCount", notificationOutbox.attemptsCount)
            .addValue("nextRetryAt", notificationOutbox.nextRetryAt)
            .addValue("updatedAt", notificationOutbox.updatedAt)
            .addValue("createdAt", notificationOutbox.createdAt)

        return namedParameterJdbcTemplate.queryForObject(
            INSERT_SQL,
            params,
            Long::class.java
        ) ?: throw IllegalStateException("Failed to insert notification and retrieve ID")
    }

    override fun update(notificationOutbox: NotificationOutbox): Long {
        val id = notificationOutbox.id
            ?: throw IllegalArgumentException("Cannot update notification without ID")

        val params = MapSqlParameterSource()
            .addValue("id", id)
            .addValue("status", notificationOutbox.status.name)
            .addValue("type", notificationOutbox.type.name)
            .addValue("payload", notificationOutbox.payload)
            .addValue("attemptsCount", notificationOutbox.attemptsCount)
            .addValue("nextRetryAt", notificationOutbox.nextRetryAt)
            .addValue("updatedAt", notificationOutbox.updatedAt)

        val rowsUpdated = namedParameterJdbcTemplate.update(UPDATE_SQL, params)

        if (rowsUpdated == 0) {
            throw IllegalStateException("NotificationOutbox with id=$id not found, update failed")
        }

        return id
    }
}
