package ru.itmo.dws.calendar.model

import org.springframework.security.core.GrantedAuthority

enum class Role(
    private val value: String,
) : GrantedAuthority {
    USER("user"),
    ADMIN("admin"),
    ;

    override fun getAuthority(): String = value

    fun getId(): Int = this.ordinal + 1
}
