package ru.itmo.dws.calendar.repository.extension

import ru.itmo.dws.calendar.model.UserRole

interface UserRolesExtension {
    fun insert(userRoles: List<UserRole>): IntArray
}
