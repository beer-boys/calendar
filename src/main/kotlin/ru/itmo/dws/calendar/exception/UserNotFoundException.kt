package ru.itmo.dws.calendar.exception

import java.util.UUID

class UserNotFoundException private constructor(
    message: String
) : NotFoundException(
    message
) {
    companion object {
        fun byUsername(username: String): UserNotFoundException {
            return UserNotFoundException("User with username = $username wasn't found")
        }

        fun byId(id: UUID): UserNotFoundException {
            return UserNotFoundException("User with id = $id wasn't found")
        }
    }
}
