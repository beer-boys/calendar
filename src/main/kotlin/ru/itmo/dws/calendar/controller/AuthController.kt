package ru.itmo.dws.calendar.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itmo.dws.calendar.dto.AuthRequest
import ru.itmo.dws.calendar.dto.AuthResponse
import ru.itmo.dws.calendar.dto.RefreshTokenRequest
import ru.itmo.dws.calendar.dto.RegisterDtoRequest
import ru.itmo.dws.calendar.dto.RegisterDtoResponse
import ru.itmo.dws.calendar.service.AuthService

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
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
}
