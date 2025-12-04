package ru.itmo.dws.calendar.service

import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.exception.UserNotFoundException
import ru.itmo.dws.calendar.model.Role
import ru.itmo.dws.calendar.model.User
import ru.itmo.dws.calendar.repository.UserRepository
import ru.itmo.dws.calendar.repository.UserRolesRepository

@Component
class UserService(
    private val userRepository: UserRepository,
    private val userRolesRepository: UserRolesRepository,
) {

    fun findByLogin(
        userLogin: String,
    ): User {
        val currentUser = userRepository.findByLogin(userLogin)
            ?: throw UserNotFoundException.byUsername(userLogin)
        val userRoles = userRolesRepository.findRolesByUserId(currentUser.id)

        val rolesEnum = userRoles.map { Role.valueOf(it) }.toSet()

        return currentUser.copy(roles = rolesEnum)
    }
}
