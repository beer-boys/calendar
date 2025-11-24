package ru.itmo.dws.calendar.dto

data class RefreshTokenRequest(
    val oldRefreshToken: String,
)
