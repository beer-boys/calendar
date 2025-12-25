package ru.itmo.dws.calendar.repository.adapter

import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.core.domain.model.HabitOccurrence
import ru.itmo.dws.calendar.core.domain.model.OccurrenceStatus
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.port.output.HabitOccurrenceRepository

@Component
class JdbcHabitOccurrenceRepositoryAdapter(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : HabitOccurrenceRepository {

    companion object {
        private const val INSERT_SQL = """
            INSERT INTO habit_occurrences (
                habit_id, occurrence_date, status, 
                slot_start, slot_end, external_event_id, reason
            ) VALUES (
                :habitId, :date, :status::occurrence_status,
                :slotStart, :slotEnd, :externalEventId, :reason
            )
            ON CONFLICT (habit_id, occurrence_date) 
            DO UPDATE SET
                status = :status::occurrence_status,
                slot_start = :slotStart,
                slot_end = :slotEnd,
                external_event_id = :externalEventId,
                reason = :reason,
                updated_at = CURRENT_TIMESTAMP
            RETURNING id, habit_id, occurrence_date, status, slot_start, slot_end, 
                      external_event_id, reason, created_at, updated_at
        """

        private const val SELECT_BY_HABIT_SQL = """
            SELECT id, habit_id, occurrence_date, status, slot_start, slot_end, 
                   external_event_id, reason, created_at, updated_at
            FROM habit_occurrences 
            WHERE habit_id = :habitId
            ORDER BY occurrence_date
        """

        private const val SELECT_BY_HABIT_AND_RANGE_SQL = """
            SELECT id, habit_id, occurrence_date, status, slot_start, slot_end, 
                   external_event_id, reason, created_at, updated_at
            FROM habit_occurrences 
            WHERE habit_id = :habitId 
              AND occurrence_date >= :startDate 
              AND occurrence_date <= :endDate
            ORDER BY occurrence_date
        """

        private const val SELECT_BY_HABIT_AND_DATE_SQL = """
            SELECT id, habit_id, occurrence_date, status, slot_start, slot_end, 
                   external_event_id, reason, created_at, updated_at
            FROM habit_occurrences 
            WHERE habit_id = :habitId AND occurrence_date = :date
        """

        private const val SELECT_BY_EXTERNAL_ID_SQL = """
            SELECT id, habit_id, occurrence_date, status, slot_start, slot_end, 
                   external_event_id, reason, created_at, updated_at
            FROM habit_occurrences 
            WHERE external_event_id = :externalEventId
        """

        private const val UPDATE_SQL = """
            UPDATE habit_occurrences SET
                status = :status::occurrence_status,
                slot_start = :slotStart,
                slot_end = :slotEnd,
                external_event_id = :externalEventId,
                reason = :reason,
                updated_at = CURRENT_TIMESTAMP
            WHERE habit_id = :habitId AND occurrence_date = :date
        """

        private const val DELETE_BY_HABIT_SQL = """
            DELETE FROM habit_occurrences WHERE habit_id = :habitId
        """

        private const val DELETE_BY_HABIT_AND_RANGE_SQL = """
            DELETE FROM habit_occurrences 
            WHERE habit_id = :habitId 
              AND occurrence_date >= :startDate 
              AND occurrence_date <= :endDate
        """

        private const val DELETE_BY_HABIT_AND_DATE_SQL = """
            DELETE FROM habit_occurrences 
            WHERE habit_id = :habitId AND occurrence_date = :date
        """
    }

    private val rowMapper = HabitOccurrenceRowMapper()

    override fun save(occurrence: HabitOccurrence): HabitOccurrence {
        val params = buildParams(occurrence)
        return jdbcTemplate.query(INSERT_SQL, params, rowMapper).first()
    }

    override fun saveAll(occurrences: List<HabitOccurrence>): List<HabitOccurrence> {
        return occurrences.map { save(it) }
    }

    override fun findByHabitId(habitId: HabitId): List<HabitOccurrence> {
        val params = MapSqlParameterSource().addValue("habitId", habitId.value)
        return jdbcTemplate.query(SELECT_BY_HABIT_SQL, params, rowMapper)
    }

    override fun findByHabitIdAndDateRange(
        habitId: HabitId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitOccurrence> {
        val params = MapSqlParameterSource()
            .addValue("habitId", habitId.value)
            .addValue("startDate", startDate)
            .addValue("endDate", endDate)
        return jdbcTemplate.query(SELECT_BY_HABIT_AND_RANGE_SQL, params, rowMapper)
    }

    override fun findByHabitIdAndDate(habitId: HabitId, date: LocalDate): HabitOccurrence? {
        val params = MapSqlParameterSource()
            .addValue("habitId", habitId.value)
            .addValue("date", date)
        return jdbcTemplate.query(SELECT_BY_HABIT_AND_DATE_SQL, params, rowMapper).firstOrNull()
    }

    override fun findByExternalEventId(externalEventId: String): HabitOccurrence? {
        val params = MapSqlParameterSource().addValue("externalEventId", externalEventId)
        return jdbcTemplate.query(SELECT_BY_EXTERNAL_ID_SQL, params, rowMapper).firstOrNull()
    }

    override fun update(occurrence: HabitOccurrence): Boolean {
        val params = buildParams(occurrence)
        return jdbcTemplate.update(UPDATE_SQL, params) > 0
    }

    override fun deleteByHabitId(habitId: HabitId): Int {
        val params = MapSqlParameterSource().addValue("habitId", habitId.value)
        return jdbcTemplate.update(DELETE_BY_HABIT_SQL, params)
    }

    override fun deleteByHabitIdAndDateRange(
        habitId: HabitId,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int {
        val params = MapSqlParameterSource()
            .addValue("habitId", habitId.value)
            .addValue("startDate", startDate)
            .addValue("endDate", endDate)
        return jdbcTemplate.update(DELETE_BY_HABIT_AND_RANGE_SQL, params)
    }

    override fun delete(occurrence: HabitOccurrence): Boolean {
        val params = MapSqlParameterSource()
            .addValue("habitId", occurrence.habitId.value)
            .addValue("date", occurrence.date)
        return jdbcTemplate.update(DELETE_BY_HABIT_AND_DATE_SQL, params) > 0
    }

    private fun buildParams(occurrence: HabitOccurrence): MapSqlParameterSource {
        return MapSqlParameterSource()
            .addValue("habitId", occurrence.habitId.value)
            .addValue("date", occurrence.date)
            .addValue("status", occurrence.status.name)
            .addValue("slotStart", occurrence.timeSlot?.start?.toOffsetDateTime())
            .addValue("slotEnd", occurrence.timeSlot?.end?.toOffsetDateTime())
            .addValue("externalEventId", occurrence.externalEventId)
            .addValue("reason", occurrence.reason)
    }

    private class HabitOccurrenceRowMapper : RowMapper<HabitOccurrence> {
        override fun mapRow(rs: ResultSet, rowNum: Int): HabitOccurrence {
            val slotStart = rs.getObject("slot_start", java.time.OffsetDateTime::class.java)
            val slotEnd = rs.getObject("slot_end", java.time.OffsetDateTime::class.java)

            val timeSlot = if (slotStart != null && slotEnd != null) {
                TimeSlot(
                    start = slotStart.toZonedDateTime(),
                    end = slotEnd.toZonedDateTime()
                )
            } else {
                null
            }

            return HabitOccurrence(
                habitId = HabitId(UUID.fromString(rs.getString("habit_id"))),
                date = rs.getObject("occurrence_date", LocalDate::class.java),
                status = OccurrenceStatus.valueOf(rs.getString("status")),
                timeSlot = timeSlot,
                reason = rs.getString("reason"),
                externalEventId = rs.getString("external_event_id")
            )
        }
    }
}
