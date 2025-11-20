package ru.itmo.dws.calendar.repository

import java.util.UUID
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.model.User

@Component
interface UserRepository : Repository<User, UUID> {

    @Query("""
        SELECT *
        FROM users
        WHERE id = :userId
    """)
    fun findById(userId: UUID): User?

    @Query("""
        SELECT id, login, password, first_name, last_name, middle_name
        FROM users
        WHERE login = :login
    """)
    fun findByLogin(login: String): User?

    @Query("""
        INSERT INTO users
        VALUES (:id, :login, :password, :firstName, :lastName, :middleName)
    """)
    @Modifying
    fun insert(
        id: UUID,
        login: String,
        password: String,
        firstName: String,
        lastName: String,
        middleName: String,
    )
}