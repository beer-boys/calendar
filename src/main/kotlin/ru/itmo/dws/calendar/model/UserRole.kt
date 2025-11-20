package ru.itmo.dws.calendar.model

import java.util.UUID

data class UserRole(
    val userId: UUID,
    val roleId: Int,
)