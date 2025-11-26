package ru.itmo.dws.calendar.exception

class UserNotFoundException private constructor(
    message: String
) : NotFoundException(
    message
) {
    companion object {
        fun byUsername(username: String): UserNotFoundException {
            return UserNotFoundException("User with username = $username wasn't found")
        }
    }
}
