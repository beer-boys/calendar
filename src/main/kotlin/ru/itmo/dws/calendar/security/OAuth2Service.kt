package ru.itmo.dws.calendar.security

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.dws.calendar.exception.LoginFailException
import ru.itmo.dws.calendar.exception.OAuth2Exception
import ru.itmo.dws.calendar.repository.UserOAuthLinkRepository

@Service
class OAuth2Service(
    private val manager: OAuth2AuthorizedClientManager,
    private val userOAuthLinkRepository: UserOAuthLinkRepository,
) {

    @Transactional(readOnly = true)
    fun exists(username: String, provider: String): Boolean {
        return userOAuthLinkRepository.existsByUserLoginAndClientRegistrationId(username, provider)
    }

    @Transactional
    fun getAccessToken(username: String, provider: String): String {
        val link = userOAuthLinkRepository.findByUserLoginAndClientRegistrationId(username, provider)
            ?: throw OAuth2Exception(username, provider)

        val authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(provider)
            .principal(link.externalPrincipalName)
            .build()

        val authorizedClient = manager.authorize(authorizeRequest) ?: throw LoginFailException(username)

        return authorizedClient.accessToken.tokenValue
    }

    /**
     * Будет актуально, когда у нас пользователь в системе будет жить неделю и более.
     */
    fun refreshAll() {
        TODO()
    }
}
