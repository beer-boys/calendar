package ru.itmo.dws.calendar.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itmo.dws.calendar.dto.UserInfoDto
import ru.itmo.dws.calendar.service.UserService

@RestController
@RequestMapping("/v1/me")
class MeController(
    private val userService: UserService,
) {

    @GetMapping
    fun getCurrentUserInfo(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<UserInfoDto> {
        val currentUser = userService.findByLogin(userDetails.username)
        return ResponseEntity.ok(
            UserInfoDto(
                currentUser.id,
                currentUser.login,
                currentUser.firstName,
                currentUser.lastName,
                currentUser.middleName,
                currentUser.roles.map { it.toString() }.toSet()
            )
        )
    }
}
