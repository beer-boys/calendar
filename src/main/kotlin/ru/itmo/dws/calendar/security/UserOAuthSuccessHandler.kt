package ru.itmo.dws.calendar.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import ru.itmo.dws.calendar.configuration.BasePath
import ru.itmo.dws.calendar.model.UserOAuthLink
import ru.itmo.dws.calendar.repository.UserOAuthLinkRepository
import ru.itmo.dws.calendar.security.jwt.JwtProvider

@Component
class UserOAuthSuccessHandler(
    private val jwtProvider: JwtProvider,
    private val linkRepository: UserOAuthLinkRepository,
) : AuthenticationSuccessHandler {

    @Transactional
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ) {
        val cookies = request.cookies
        val jwtCookie = cookies?.find { it.name == JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE }

        if (jwtCookie != null && jwtProvider.isAccessTokenValid(jwtCookie.value)) {
            val localUserLogin = jwtProvider.extractUsername(jwtCookie.value)

            if (authentication is OAuth2AuthenticationToken) {
                val registrationId = authentication.authorizedClientRegistrationId
                val externalId = authentication.name

                val existingLink = linkRepository.findByUserLoginAndClientRegistrationId(
                    localUserLogin,
                    registrationId
                )

                val linkToSave = existingLink?.copy(externalPrincipalName = externalId)
                    ?: UserOAuthLink(
                        userLogin = localUserLogin,
                        clientRegistrationId = registrationId,
                        externalPrincipalName = externalId
                    )

                linkRepository.save(linkToSave)

                response.sendRedirect("${BasePath.GOOGLE_BASE}/calendars")
            } else {
                response.sendRedirect("/auth/error?reason=not_oauth_token")
            }
        } else {
            response.sendRedirect("/auth/error?reason=jwt_invalid")
        }
    }
}
