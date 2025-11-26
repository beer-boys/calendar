package ru.itmo.dws.calendar.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import ru.itmo.dws.calendar.configuration.BasePath
import ru.itmo.dws.calendar.configuration.BasePath.GOOGLE_WHITE_LIST
import ru.itmo.dws.calendar.configuration.BasePath.WHITE_LIST

@Configuration
@EnableWebSecurity
@Suppress("SpreadOperator", "MagicNumber", "ForbiddenComment")
open class SecurityConfiguration {

    @Bean
    @Order(10)
    open fun baseAuthChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
    ): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers(*WHITE_LIST.toTypedArray()).permitAll()
                it.requestMatchers("${BasePath.BASE}/**").authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    @Order(1)
    open fun googleOAuth2Chain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .securityMatcher(*GOOGLE_WHITE_LIST.toTypedArray())
            .authorizeHttpRequests {
                it.anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2.successHandler { _, response, _ ->
                    // todo maybe change habit for another in future
                    response.sendRedirect("${BasePath.GOOGLE_BASE}/calendars")
                }
            }
            .build()
    }

    @Bean
    open fun bCryptPasswordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    open fun authenticationProvider(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder
    ): AuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setUserDetailsService(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder)
        return provider
    }

    @Bean
    open fun authenticationManager(
        configuration: AuthenticationConfiguration,
        authenticationProvider: AuthenticationProvider,
    ): AuthenticationManager {
        return configuration.authenticationManager
    }

    @Bean
    open fun roleHierarchy(): RoleHierarchy {
        val roleHierarchy = RoleHierarchyImpl.fromHierarchy(
            "ADMIN > USER"
        )
        return roleHierarchy
    }
}
