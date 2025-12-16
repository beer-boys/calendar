package ru.itmo.dws.calendar.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.itmo.dws.calendar.model.UserOAuthLink

@Repository
interface UserOAuthLinkRepository : CrudRepository<UserOAuthLink, Long> {

    fun findByUserLoginAndClientRegistrationId(
        userLogin: String,
        clientRegistrationId: String,
    ): UserOAuthLink?
}
