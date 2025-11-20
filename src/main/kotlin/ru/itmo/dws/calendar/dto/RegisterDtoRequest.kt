package ru.itmo.dws.calendar.dto

import java.time.Instant
import java.util.UUID

data class RegisterDtoRequest(
    val login: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val middleName: String,
)

data class RegisterDtoResponse(
    val id: UUID,
    val login: String,
    val firstName: String,
    val lastName: String,
    val middleName: String,
    val roles: Set<String>,
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant
)