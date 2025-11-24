package ru.itmo.dws.calendar.repository.extension

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.model.UserRole

@Component
class UserRolesExtensionImpl(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) : UserRolesExtension {

    companion object {
        private const val INSERT_SQL = """
            INSERT INTO users_roles
            VALUES (:userId, :roleId)
        """
    }

    override fun insert(
        userRoles: List<UserRole>
    ): IntArray {
        val parameters = userRoles.map {
            MapSqlParameterSource()
                .addValue("userId", it.userId)
                .addValue("roleId", it.roleId)
        }
            .toList()

        return namedParameterJdbcTemplate.batchUpdate(INSERT_SQL, parameters.toTypedArray())
    }
}
