package ru.itmo.dws.calendar.repository

import java.util.UUID
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.model.UserRole
import ru.itmo.dws.calendar.repository.extension.UserRolesExtension

@Component
interface UserRolesRepository : UserRolesExtension, Repository<UserRole, Void> {

    @Query(
        """
        INSERT INTO users_roles
        VALUES (:userId, :roleId)
    """
    )
    @Modifying
    fun insert(userId: UUID, roleId: Int)

    @Query(
        """
        SELECT r.role_name
        FROM roles r
        WHERE r.id IN (
            SELECT role_id
            FROM users_roles ur
            WHERE ur.user_id = :userId
        )
    """
    )
    fun findRolesByUserId(userId: UUID): List<Int>
}
