package ru.itmo.dws.calendar.repository.extension

import java.util.UUID
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomSearchCriteria
import ru.itmo.dws.calendar.model.MeetingRoomEntity
import ru.itmo.dws.calendar.model.types.JsonbString

@Repository
class MeetingRoomRepositoryExtensionImpl(
    private val aggregateTemplate: JdbcAggregateTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) : MeetingRoomRepositoryExtension {

    override fun insert(room: MeetingRoomEntity): MeetingRoomEntity {
        return aggregateTemplate.insert(room)
    }

    override fun findAllByCriteria(criteria: MeetingRoomSearchCriteria): List<MeetingRoomEntity> {
        val sql = StringBuilder()
        val params = mutableMapOf<String, Any>()

        sql.appendLine("select * from meeting_rooms")
        sql.appendLine("where 1=1")

        criteria.minCapacity?.let {
            sql.appendLine("and capacity >= :minCapacity")
            params["minCapacity"] = it
        }
        criteria.status?.let {
            sql.appendLine("and status = :status")
            params["status"] = it.name
        }
        criteria.floor?.let {
            sql.appendLine("and floor = :floor")
            params["floor"] = it
        }
        criteria.building?.let {
            sql.appendLine("and building = :building")
            params["building"] = it
        }

        criteria.locationQuery?.takeIf { it.isNotBlank() }?.let {
            sql.appendLine(
                """
                and (
                  coalesce(address,'') ilike :q
                  or coalesce(building,'') ilike :q
                  or coalesce(wing,'') ilike :q
                  or coalesce(city,'') ilike :q
                  or coalesce(room_number,'') ilike :q
                )
                """.trimIndent()
            )
            params["q"] = "%${it.trim()}%"
        }

        if (criteria.requiredFeatures.isNotEmpty()) {
            sql.appendLine(
                """
                and (features_json -> 'features') @> :requiredFeaturesJson::jsonb
                """.trimIndent()
            )
            val jsonArray = criteria.requiredFeatures.joinToString(
                prefix = "[\"",
                postfix = "\"]",
                separator = "\",\""
            ) { it.name }
            params["requiredFeaturesJson"] = jsonArray
        }

        sql.appendLine("order by name asc")

        return namedParameterJdbcTemplate.query(sql.toString(), params) { rs, _ ->
            MeetingRoomEntity(
                id = rs.getObject("id", UUID::class.java),
                name = rs.getString("name"),
                capacity = rs.getInt("capacity"),
                status = rs.getString("status"),
                address = rs.getString("address"),
                building = rs.getString("building"),
                floor = rs.getObject("floor") as Int?,
                wing = rs.getString("wing"),
                roomNumber = rs.getString("room_number"),
                city = rs.getString("city"),
                timeZone = rs.getString("time_zone"),
                featuresJson = JsonbString(rs.getString("features_json")),
            )
        }
    }
}
