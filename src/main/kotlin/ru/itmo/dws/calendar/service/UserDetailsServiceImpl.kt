package ru.itmo.dws.calendar.service

import java.util.*
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.exception.UserNotFoundException
import ru.itmo.dws.calendar.repository.UserRepository

@Component
class UserDetailsServiceImpl(
    private val userRepository: UserRepository,
) : UserDetailsService {

    override fun loadUserByUsername(userId: String): UserDetails {
        return userRepository.findById(UUID.fromString(userId))
            ?: throw UserNotFoundException(UUID.fromString(userId))
    }
}
