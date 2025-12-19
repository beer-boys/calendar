package ru.itmo.dws.calendar.service

import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.dws.calendar.model.Role
import ru.itmo.dws.calendar.model.UserRole
import ru.itmo.dws.calendar.repository.UserRepository
import ru.itmo.dws.calendar.repository.UserRolesRepository

@Service
class OAuth2UserProvisioningService(
    private val userRepository: UserRepository,
    private val userRolesRepository: UserRolesRepository
) : DefaultOAuth2UserService() {

    private val log = LoggerFactory.getLogger(OAuth2UserProvisioningService::class.java)

    companion object {
        private const val OAUTH2_PASSWORD_PLACEHOLDER = "oauth2-no-password"
        private const val OAUTH2_MIDDLE_NAME_PLACEHOLDER = "-"
        private const val UNKNOWN_NAME = "Unknown"
    }

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)

        val email = oAuth2User.getAttribute<String>("email")
        val name = oAuth2User.getAttribute<String>("name") ?: ""
        val givenName = oAuth2User.getAttribute<String>("given_name") ?: ""
        val familyName = oAuth2User.getAttribute<String>("family_name") ?: ""

        if (email != null) {
            provisionUser(email, givenName, familyName, name)
        }

        return oAuth2User
    }

    private fun provisionUser(email: String, firstName: String, lastName: String, fullName: String) {
        val userId = UUID.nameUUIDFromBytes(email.toByteArray())

        val existingUser = userRepository.findById(userId)
        if (existingUser != null) {
            log.debug("User {} already exists", email)
            return
        }

        log.info("Creating new user from OAuth2: {}", email)

        userRepository.insert(
            id = userId,
            login = email,
            password = OAUTH2_PASSWORD_PLACEHOLDER,
            firstName = firstName.ifBlank { fullName.split(" ").firstOrNull() ?: UNKNOWN_NAME },
            lastName = lastName.ifBlank { fullName.split(" ").drop(1).joinToString(" ").ifBlank { UNKNOWN_NAME } },
            middleName = OAUTH2_MIDDLE_NAME_PLACEHOLDER
        )

        userRolesRepository.insert(
            listOf(
                UserRole(userId = userId, roleId = Role.USER.getId())
            )
        )

        log.info("Created user {} with id {}", email, userId)
    }
}
