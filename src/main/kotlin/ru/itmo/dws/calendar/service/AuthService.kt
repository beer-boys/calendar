package ru.itmo.dws.calendar.service

import java.time.Instant
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.dws.calendar.dto.AuthRequest
import ru.itmo.dws.calendar.dto.AuthResponse
import ru.itmo.dws.calendar.dto.RegisterDtoRequest
import ru.itmo.dws.calendar.dto.RegisterDtoResponse
import ru.itmo.dws.calendar.exception.LoginFailException
import ru.itmo.dws.calendar.exception.RegisterFailException
import ru.itmo.dws.calendar.model.Role
import ru.itmo.dws.calendar.model.User
import ru.itmo.dws.calendar.model.UserRole
import ru.itmo.dws.calendar.repository.UserRepository
import ru.itmo.dws.calendar.repository.UserRolesRepository
import ru.itmo.dws.calendar.security.jwt.JwtProvider

@Service
open class AuthService(
    private val userRepository: UserRepository,
    private val userRolesRepository: UserRolesRepository,
    private val bCryptPasswordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val authenticationManager: AuthenticationManager,
) {

    @Autowired
    @Lazy
    private lateinit var self: AuthService

    open fun login(request: AuthRequest): AuthResponse {
        try {
            val existedUser = userRepository.findByLogin(request.login)
                ?: throw LoginFailException(request.login)

            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(existedUser.id, request.password)
            )

            return generateTokenResponse(existedUser)

        } catch (e: AuthenticationException) {
            throw LoginFailException(request.login)
        }
    }

    @Transactional
    open fun registerUserAndGetTokens(request: RegisterDtoRequest): RegisterDtoResponse? {
        val user = createUser(request)
        val response = self.register(request, user)
        val tokenResponse = generateTokenResponse(user)

        return RegisterDtoResponse(
            id = response.id,
            login = response.login,
            firstName = response.firstName,
            lastName = response.lastName,
            middleName = response.middleName,
            roles = response.roles,
            accessToken = tokenResponse.accessToken,
            accessTokenExpiresAt = tokenResponse.accessTokenExpiresAt,
            refreshToken = tokenResponse.refreshToken,
            refreshTokenExpiresAt = tokenResponse.refreshTokenExpiresAt
        )
    }

    open fun getNewAccessToken(oldRefreshToken: String): AuthResponse {
        if (jwtProvider.isRefreshTokenValid(oldRefreshToken)) {
            val claims = jwtProvider.getClaims(oldRefreshToken)
            val userLogin = claims.subject
            val existedUser = userRepository.findByLogin(userLogin)
                ?: throw LoginFailException(userLogin)

            return generateTokenResponse(existedUser)
        }

        throw RuntimeException("")
    }

    @Transactional
    open fun register(request: RegisterDtoRequest, user: User): RegisterDtoResponse {
        if (userRepository.findByLogin(request.login) != null) {
            throw RegisterFailException(request.login)
        }

        val userRoles = user.roles.map {
            UserRole(
                userId = user.id,
                roleId = it.getId()
            )
        }

        userRepository.insert(
            id = user.id,
            login = user.login,
            password = user.hashedPassword,
            firstName = user.firstName,
            lastName = user.lastName,
            middleName = user.middleName,
        )
        userRolesRepository.insert(userRoles)

        return RegisterDtoResponse(
            id = user.id,
            login = user.login,
            firstName = user.firstName,
            lastName = user.lastName,
            middleName = user.middleName,
            roles = user.roles.map { it.toString() }.toSet(),
            accessToken = "",
            accessTokenExpiresAt = Instant.now(),
            refreshToken = "",
            refreshTokenExpiresAt = Instant.now()
        )
    }

    private fun createUser(request: RegisterDtoRequest): User {
         return User(
            id = UUID.randomUUID(),
            login = request.login,
            hashedPassword = bCryptPasswordEncoder.encode(request.password),
            firstName = request.firstName,
            lastName = request.lastName,
            middleName = request.middleName,
            roles = mutableSetOf(Role.USER)
        )
    }

    private fun generateTokenResponse(existedUser: User): AuthResponse {
        val accessToken = jwtProvider.generateAccessToken(existedUser)
        val refreshToken = jwtProvider.generateRefreshToken(existedUser)
        val accessTokenExpiresAt = jwtProvider.extractClaim(
            token = accessToken
        ) { it.expiration }
        val refreshTokenExpiresAt = jwtProvider.extractClaim(
            token = refreshToken
        ) { it.expiration }

        return AuthResponse(
            accessToken = accessToken,
            accessTokenExpiresAt = accessTokenExpiresAt.toInstant(),
            refreshToken = refreshToken,
            refreshTokenExpiresAt = refreshTokenExpiresAt.toInstant()
        )
    }
}