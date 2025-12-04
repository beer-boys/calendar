package ru.itmo.dws.calendar.configuration

object BasePath {
    const val BASE = "/api/v1"
    const val READYZ = "/readyz"
    const val AUTH = "$BASE/auth/**"
    const val GOOGLE_BASE = "$BASE/google"
    const val GOOGLE_OAUTH2 = "/oauth2/authorization/google"
    const val GOOGLE_CALLBACK = "/login/oauth2/code/google"

    val WHITE_LIST = listOf(AUTH, READYZ, "$BASE/me")
    val GOOGLE_WHITE_LIST = listOf(
        "$GOOGLE_BASE/**",
        "$GOOGLE_OAUTH2/**",
        "$GOOGLE_CALLBACK/**"
    )
}
