package ru.itmo.dws.calendar.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("user_oauth_links")
data class UserOAuthLink(
    @Id
    val id: Long? = null, // spring data jdbc support composite keys only from 4.0.0
    @Column("user_login")
    val userLogin: String,
    @Column("client_registration_id")
    val clientRegistrationId: String,
    @Column("external_principal_name")
    val externalPrincipalName: String
)
