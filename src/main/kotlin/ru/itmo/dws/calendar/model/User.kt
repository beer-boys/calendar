package ru.itmo.dws.calendar.model

import java.util.UUID
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Table("users")
data class User(
    val id: UUID,
    val login: String,
    @Column("password")
    val hashedPassword: String,
    val firstName: String,
    val lastName: String,
    val middleName: String,
    val roles: Set<Role> = emptySet(),
) : UserDetails {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> =
        roles.toMutableList()

    override fun getPassword(): String = hashedPassword

    override fun getUsername(): String = id.toString()
}
