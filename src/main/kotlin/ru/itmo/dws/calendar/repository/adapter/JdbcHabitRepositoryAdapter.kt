package ru.itmo.dws.calendar.repository.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.core.domain.model.Habit
import ru.itmo.dws.calendar.core.domain.valueobject.BufferDuration
import ru.itmo.dws.calendar.core.domain.valueobject.DateRange
import ru.itmo.dws.calendar.core.domain.valueobject.DayTimeExclusion
import ru.itmo.dws.calendar.core.domain.valueobject.HabitFlexibilityWindow
import ru.itmo.dws.calendar.core.domain.valueobject.HabitId
import ru.itmo.dws.calendar.core.domain.valueobject.Priority
import ru.itmo.dws.calendar.core.domain.valueobject.RecurrenceRule
import ru.itmo.dws.calendar.core.domain.valueobject.SchedulingRule
import ru.itmo.dws.calendar.core.domain.valueobject.TimeRange
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlotOverride
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.HabitRepository
import ru.itmo.dws.calendar.repository.entity.DayTimeExclusionDto
import ru.itmo.dws.calendar.repository.entity.ExclusionRuleDto
import ru.itmo.dws.calendar.repository.entity.FrequencyRuleDto
import ru.itmo.dws.calendar.repository.entity.HabitMetadata
import ru.itmo.dws.calendar.repository.entity.RecurrenceExceptionRuleDto
import ru.itmo.dws.calendar.repository.entity.SchedulingRuleDto
import ru.itmo.dws.calendar.repository.entity.TimeRangeDto
import ru.itmo.dws.calendar.repository.entity.TimeSlotOverrideDto
import ru.itmo.dws.calendar.repository.entity.TimeWindowRuleDto

@Component
class JdbcHabitRepositoryAdapter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper
) : HabitRepository {

    companion object {
        private const val ENTITY_TYPE = "HABIT"

        private const val INSERT_SQL = """
            INSERT INTO calendar_events (
                id, user_id, entity_type, 
                title, description, priority, metadata, 
                created_at, updated_at
            ) VALUES (
                :id, :userId, :entityType::entity_type,
                :title, :description, :priority, :metadata::jsonb,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
        """

        private const val SELECT_BY_ID_SQL = """
            SELECT id, user_id, entity_type, 
                   title, description, priority, metadata,
                   created_at, updated_at
            FROM calendar_events 
            WHERE id = :id AND entity_type = 'HABIT'
        """

        private const val SELECT_BY_USER_SQL = """
            SELECT id, user_id, entity_type, 
                   title, description, priority, metadata,
                   created_at, updated_at
            FROM calendar_events 
            WHERE user_id = :userId AND entity_type = 'HABIT'
        """

        private const val SELECT_ALL_SQL = """
            SELECT id, user_id, entity_type, 
                   title, description, priority, metadata,
                   created_at, updated_at
            FROM calendar_events 
            WHERE entity_type = 'HABIT'
        """

        private const val UPDATE_SQL = """
            UPDATE calendar_events SET
                title = :title,
                description = :description,
                priority = :priority,
                metadata = :metadata::jsonb,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :id AND entity_type = 'HABIT'
        """

        private const val DELETE_SQL = """
            DELETE FROM calendar_events 
            WHERE id = :id AND entity_type = 'HABIT'
        """
    }

    private val habitRowMapper = HabitRowMapper(objectMapper)

    override fun saveHabit(habit: Habit): HabitId {
        val metadata = toMetadata(habit)
        val metadataJson = objectMapper.writeValueAsString(metadata)

        val params = MapSqlParameterSource()
            .addValue("id", habit.id.value)
            .addValue("userId", habit.userId.value)
            .addValue("entityType", ENTITY_TYPE)
            .addValue("title", habit.title)
            .addValue("description", habit.description)
            .addValue("priority", habit.priority.value)
            .addValue("metadata", metadataJson)

        jdbcTemplate.update(INSERT_SQL, params)
        return habit.id
    }

    override fun findHabit(habitId: HabitId): Habit? {
        val params = MapSqlParameterSource()
            .addValue("id", habitId.value)

        return jdbcTemplate.query(SELECT_BY_ID_SQL, params, habitRowMapper)
            .firstOrNull()
    }

    override fun findHabits(userId: UserId): List<Habit> {
        val params = MapSqlParameterSource()
            .addValue("userId", userId.value)

        return jdbcTemplate.query(SELECT_BY_USER_SQL, params, habitRowMapper)
    }

    override fun findHabitsForDate(userId: UserId, date: LocalDate): List<Habit> {
        return findHabits(userId).filter { it.shouldOccurOn(date) }
    }

    override fun findAllHabits(): List<Habit> {
        return jdbcTemplate.query(SELECT_ALL_SQL, habitRowMapper)
    }

    override fun updateHabit(habitId: HabitId, habit: Habit): Boolean {
        val metadata = toMetadata(habit)
        val metadataJson = objectMapper.writeValueAsString(metadata)

        val params = MapSqlParameterSource()
            .addValue("id", habitId.value)
            .addValue("title", habit.title)
            .addValue("description", habit.description)
            .addValue("priority", habit.priority.value)
            .addValue("metadata", metadataJson)

        return jdbcTemplate.update(UPDATE_SQL, params) > 0
    }

    override fun deleteHabit(habitId: HabitId): Boolean {
        val params = MapSqlParameterSource()
            .addValue("id", habitId.value)

        return jdbcTemplate.update(DELETE_SQL, params) > 0
    }

    private fun toMetadata(habit: Habit): HabitMetadata {
        return HabitMetadata(
            durationMinutes = habit.duration.toMinutes(),
            recurrenceFrequency = habit.recurrenceRule.frequency.name,
            recurrenceDaysOfWeek = habit.recurrenceRule.daysOfWeek.ifEmpty { null },
            recurrenceInterval = habit.recurrenceRule.interval,
            recurrenceStartDate = habit.recurrenceRule.startDate,
            recurrenceEndDate = habit.recurrenceRule.endDate,
            flexibilityEarliestTime = habit.flexibilityWindow.earliestTime,
            flexibilityLatestTime = habit.flexibilityWindow.latestTime,
            flexibilityAllowCrossDay = habit.flexibilityWindow.allowCrossDayMove,
            flexibilityPreferredDurationMinutes = habit.flexibilityWindow.preferredDuration?.toMinutes(),
            bufferBeforeMinutes = habit.bufferTime.before.toMinutes(),
            bufferAfterMinutes = habit.bufferTime.after.toMinutes(),
            currentTimeSlotStart = habit.currentTimeSlot?.start?.toString(),
            currentTimeSlotEnd = habit.currentTimeSlot?.end?.toString(),
            rules = habit.schedulingRules.map { toRuleDto(it) }.ifEmpty { null }
        )
    }

    private fun toRuleDto(rule: SchedulingRule): SchedulingRuleDto {
        return when (rule) {
            is SchedulingRule.TimeWindowRule -> TimeWindowRuleDto(
                earliestTime = rule.earliestTime,
                latestTime = rule.latestTime,
                activeDateRangeStart = rule.activeDateRange?.start,
                activeDateRangeEnd = rule.activeDateRange?.end,
                activeDaysOfWeek = rule.activeDaysOfWeek
            )
            is SchedulingRule.ExclusionRule -> ExclusionRuleDto(
                excludedDates = rule.excludedDates.ifEmpty { null },
                excludedDaysOfWeek = rule.excludedDaysOfWeek.ifEmpty { null },
                excludedTimeRanges = rule.excludedTimeRanges.map { dayExclusion ->
                    DayTimeExclusionDto(
                        dayOfWeek = dayExclusion.dayOfWeek,
                        excludedRanges = dayExclusion.excludedRanges.map { range ->
                            TimeRangeDto(start = range.start, end = range.end)
                        }.ifEmpty { null }
                    )
                }.ifEmpty { null },
                excludeHolidays = rule.excludeHolidays
            )
            is SchedulingRule.FrequencyRule -> FrequencyRuleDto(
                periodDays = rule.period.toDays(),
                minOccurrences = rule.minOccurrences,
                maxOccurrences = rule.maxOccurrences,
                minGapMinutes = rule.minGapBetweenOccurrences?.toMinutes()
            )
            is SchedulingRule.RecurrenceExceptionRule -> RecurrenceExceptionRuleDto(
                cancelledDates = rule.cancelledDates.ifEmpty { null },
                modifiedOccurrences = rule.modifiedOccurrences.mapKeys { it.key.toString() }
                    .mapValues { (_, override) ->
                        TimeSlotOverrideDto(
                            newStartTime = override.newStartTime,
                            newEndTime = override.newEndTime,
                            newDurationMinutes = override.newDuration?.toMinutes()
                        )
                    }.ifEmpty { null }
            )
        }
    }

    private class HabitRowMapper(
        private val objectMapper: ObjectMapper
    ) : RowMapper<Habit> {
        override fun mapRow(rs: ResultSet, rowNum: Int): Habit {
            val metadataJson = rs.getString("metadata")
            val metadata = objectMapper.readValue(metadataJson, HabitMetadata::class.java)

            val recurrenceRule = RecurrenceRule(
                frequency = RecurrenceRule.Frequency.valueOf(metadata.recurrenceFrequency),
                daysOfWeek = metadata.recurrenceDaysOfWeek ?: emptySet(),
                interval = metadata.recurrenceInterval,
                startDate = metadata.recurrenceStartDate,
                endDate = metadata.recurrenceEndDate
            )

            val flexibilityWindow = HabitFlexibilityWindow(
                earliestTime = metadata.flexibilityEarliestTime,
                latestTime = metadata.flexibilityLatestTime,
                allowCrossDayMove = metadata.flexibilityAllowCrossDay,
                preferredDuration = metadata.flexibilityPreferredDurationMinutes?.let {
                    Duration.ofMinutes(it)
                }
            )

            val bufferTime = BufferDuration(
                before = Duration.ofMinutes(metadata.bufferBeforeMinutes),
                after = Duration.ofMinutes(metadata.bufferAfterMinutes)
            )

            val currentTimeSlot = if (
                metadata.currentTimeSlotStart != null && metadata.currentTimeSlotEnd != null
            ) {
                TimeSlot(
                    start = ZonedDateTime.parse(metadata.currentTimeSlotStart),
                    end = ZonedDateTime.parse(metadata.currentTimeSlotEnd)
                )
            } else {
                null
            }

            val rules = metadata.rules?.map { fromRuleDto(it) } ?: emptyList()

            return Habit(
                id = HabitId(UUID.fromString(rs.getString("id"))),
                userId = UserId(UUID.fromString(rs.getString("user_id"))),
                title = rs.getString("title"),
                description = rs.getString("description"),
                duration = Duration.ofMinutes(metadata.durationMinutes),
                recurrenceRule = recurrenceRule,
                flexibilityWindow = flexibilityWindow,
                priority = Priority(rs.getInt("priority")),
                currentTimeSlot = currentTimeSlot,
                bufferTime = bufferTime,
                schedulingRules = rules
            )
        }

        private fun fromRuleDto(dto: SchedulingRuleDto): SchedulingRule {
            return when (dto) {
                is TimeWindowRuleDto -> SchedulingRule.TimeWindowRule(
                    earliestTime = dto.earliestTime,
                    latestTime = dto.latestTime,
                    activeDateRange = if (dto.activeDateRangeStart != null || dto.activeDateRangeEnd != null) {
                        DateRange(dto.activeDateRangeStart, dto.activeDateRangeEnd)
                    } else {
                        null
                    },
                    activeDaysOfWeek = dto.activeDaysOfWeek
                )
                is ExclusionRuleDto -> SchedulingRule.ExclusionRule(
                    excludedDates = dto.excludedDates ?: emptySet(),
                    excludedDaysOfWeek = dto.excludedDaysOfWeek ?: emptySet(),
                    excludedTimeRanges = dto.excludedTimeRanges?.map { dayDto ->
                        DayTimeExclusion(
                            dayOfWeek = dayDto.dayOfWeek,
                            excludedRanges = dayDto.excludedRanges?.map { rangeDto ->
                                TimeRange(start = rangeDto.start, end = rangeDto.end)
                            } ?: emptyList()
                        )
                    } ?: emptyList(),
                    excludeHolidays = dto.excludeHolidays
                )
                is FrequencyRuleDto -> SchedulingRule.FrequencyRule(
                    period = Duration.ofDays(dto.periodDays),
                    minOccurrences = dto.minOccurrences,
                    maxOccurrences = dto.maxOccurrences,
                    minGapBetweenOccurrences = dto.minGapMinutes?.let { Duration.ofMinutes(it) }
                )
                is RecurrenceExceptionRuleDto -> SchedulingRule.RecurrenceExceptionRule(
                    cancelledDates = dto.cancelledDates ?: emptySet(),
                    modifiedOccurrences = dto.modifiedOccurrences?.mapKeys {
                        LocalDate.parse(it.key)
                    }?.mapValues { (_, overrideDto) ->
                        TimeSlotOverride(
                            newStartTime = overrideDto.newStartTime,
                            newEndTime = overrideDto.newEndTime,
                            newDuration = overrideDto.newDurationMinutes?.let { Duration.ofMinutes(it) }
                        )
                    } ?: emptyMap()
                )
            }
        }
    }
}
