package ru.itmo.dws.calendar.exception

import java.util.UUID

class UserNotFoundException(
    id: UUID
) : NotFoundException(
    "User with id = $id wasn't found"
)
