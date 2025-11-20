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
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader(AUTH_HEADER)

        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(BEARER.length)
        val isTokenValid = jwtProvider.isAccessTokenValid(token)

        if (!isTokenValid) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val username = jwtProvider.extractUsername(token)
            if (SecurityContextHolder.getContext().authentication == null) {
                val userDetails = userDetailsService.loadUserByUsername(username)
                val authentication = UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.authorities
                ).apply {
                    details = WebAuthenticationDetailsSource().buildDetails(request)
                }
                SecurityContextHolder.getContext().authentication = authentication
            }

        } catch (expected: Exception) {
            filterChain.doFilter(request, response)
            log.error("Error while extracting username from jwt token $expected")
            return
        }

    }
}