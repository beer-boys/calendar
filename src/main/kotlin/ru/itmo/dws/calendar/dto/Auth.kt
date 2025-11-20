package ru.itmo.dws.calendar.dto

import java.time.Instant

data class AuthRequest(
    val login: String,
    val password: String
)

data class AuthResponse(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant
)