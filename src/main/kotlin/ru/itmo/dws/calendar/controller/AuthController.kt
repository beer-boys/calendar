package ru.itmo.dws.calendar.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itmo.dws.calendar.dto.AuthRequest
import ru.itmo.dws.calendar.dto.AuthResponse
import ru.itmo.dws.calendar.dto.LinkResponse
import ru.itmo.dws.calendar.dto.RefreshTokenRequest
import ru.itmo.dws.calendar.dto.RegisterDtoRequest
import ru.itmo.dws.calendar.dto.RegisterDtoResponse
import ru.itmo.dws.calendar.security.JwtAuthenticationFilter.Companion.ACCESS_TOKEN_COOKIE
import ru.itmo.dws.calendar.security.OAuth2Service
import ru.itmo.dws.calendar.service.AuthService

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val oAuth2Service: OAuth2Service,
) {

    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterDtoRequest): ResponseEntity<RegisterDtoResponse> {
        val response = authService.registerUserAndGetTokens(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/refresh")
    fun getRefreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        val response = authService.getNewAccessToken(request.oldRefreshToken)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/link/google")
    fun initGoogleAuth(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<LinkResponse> {
        if (oAuth2Service.exists(request.userPrincipal?.name!!, "google")) {
            return ResponseEntity.ok(LinkResponse(true))
        }

        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        val token = if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else {
            return ResponseEntity.status(401).build()
        }

        val cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
            .path("/")
            .httpOnly(true)
            .maxAge(300)
            .sameSite("Lax")
            // .secure(true)
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())

        return ResponseEntity.ok(LinkResponse(false, "/oauth2/authorization/google"))
    }
}
