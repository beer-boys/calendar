package ru.itmo.dws.calendar.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import java.security.Key
import java.time.ZoneOffset
import java.util.Date
import javax.crypto.SecretKey
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.model.User
import ru.itmo.dws.calendar.service.util.ClockService

@Component
class JwtProvider(
    private val clockService: ClockService,
    @Value("\${jwt.secret.access}") accessSecret: String,
    @Value("\${jwt.secret.refresh}") refreshSecret: String
) {
    private val accessKey: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecret))
    private val refreshKey: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret))

    companion object {
        private const val MONTH_IN_MINUTES: Long = 60 * 24 * 30
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    fun extractUsername(token: String): String =
        extractClaim(token) { it.subject }

    fun <T> extractClaim(token: String, resolver: (Claims) -> T): T =
        resolver(getClaims(token))

    fun generateAccessToken(
        user: User,
    ): String {
        return generateToken(
            subject = user.login,
            expiresOffsetInMinutes = 30,
            key = accessKey,
            mapOf(
                "first_name" to user.firstName,
                "last_name" to user.lastName,
                "middle_name" to user.middleName,
                "user_login" to user.login,
                "user_roles" to user.roles
            )
        )
    }

    fun generateRefreshToken(
        user: User
    ): String {
        return generateToken(
            subject = user.login,
            expiresOffsetInMinutes = MONTH_IN_MINUTES,
            key = refreshKey
        )
    }

    fun isAccessTokenValid(token: String): Boolean =
        validateToken(token, accessKey)

    fun isRefreshTokenValid(token: String): Boolean =
        validateToken(token, refreshKey)

    private fun validateToken(
        token: String,
        key: Key,
    ): Boolean {
        return try {
            Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            true
        } catch (expected: Exception) {
            log.error("Error while validating token = $expected")
            false
        }
    }

    private fun generateToken(
        subject: String,
        expiresOffsetInMinutes: Long,
        key: Key,
        claims: Map<String, Any> = emptyMap()
    ): String {
        val expiration = Date.from(
            clockService.now()
                .plusMinutes(expiresOffsetInMinutes)
                .atOffset(ZoneOffset.UTC)
                .toInstant()
        )

        return Jwts.builder()
            .subject(subject)
            .expiration(expiration)
            .claims(claims)
            .signWith(key)
            .compact()
    }

    fun getClaims(token: String): Claims {
        return getClaims(token, accessKey)
    }

    private fun getClaims(token: String, key: Key): Claims {
        return Jwts.parser()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }
}
