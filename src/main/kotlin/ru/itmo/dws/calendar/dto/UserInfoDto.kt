package ru.itmo.dws.calendar.dto

import java.util.UUID

data class UserInfoDto(
    val id: UUID,
    val login: String,
    val firstName: String,
    val lastName: String,
    val middleName: String,
    val roles: Set<String>,
)
