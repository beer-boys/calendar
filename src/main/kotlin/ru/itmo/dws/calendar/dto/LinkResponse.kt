package ru.itmo.dws.calendar.dto

data class LinkResponse(
    val connected: Boolean,
    val redirectUrl: String? = null,
)
