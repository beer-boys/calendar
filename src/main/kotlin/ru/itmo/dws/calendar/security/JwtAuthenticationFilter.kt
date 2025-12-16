package ru.itmo.dws.calendar.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ru.itmo.dws.calendar.security.jwt.JwtProvider

@Component
class JwtAuthenticationFilter(
    private val userDetailsService: UserDetailsService,
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {

    companion object {
        private const val BEARER = "Bearer "
        private const val AUTH_HEADER = "Authorization"
        const val ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN_COOKIE"
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    @Suppress("ReturnCount")
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)

        if (token == null || jwtProvider.isAccessTokenValid(token).not()) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val username = jwtProvider.extractUsername(token)
            if (SecurityContextHolder.getContext().authentication == null) {
                val userDetails = userDetailsService.loadUserByUsername(username)
                val authentication = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.authorities
                ).apply {
                    details = WebAuthenticationDetailsSource().buildDetails(request)
                }
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (expected: Exception) {
            log.error("Error while extracting username from jwt token $expected")
        } finally {
            filterChain.doFilter(request, response)
        }
    }

    /**
     * Resolve token from request.
     *
     * First try to resolve from Authorization header, then from cookie for short-term google oauth2 redirect.
     */
    private fun resolveToken(request: HttpServletRequest): String? {
        val authHeader = request.getHeader(AUTH_HEADER)
        if (authHeader != null && authHeader.startsWith(BEARER)) {
            return authHeader.substring(BEARER.length)
        }

        val cookies = request.cookies
        if (cookies != null) {
            return cookies.find { it.name == ACCESS_TOKEN_COOKIE }?.value
        }

        return null
    }
}
