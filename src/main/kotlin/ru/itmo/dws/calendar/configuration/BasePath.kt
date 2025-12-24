package ru.itmo.dws.calendar.configuration

object BasePath {
    const val BASE = "/v1"
    const val READYZ = "/readyz"
    const val AUTH_LOGIN = "$BASE/auth/login"
    const val AUTH_REGISTER = "$BASE/auth/register"
    const val AUTH_REFRESH = "$BASE/auth/refresh"

    const val GOOGLE_BASE = "$BASE/google"
    const val GOOGLE_OAUTH2 = "/oauth2/authorization/google"
    const val GOOGLE_CALLBACK = "/login/oauth2/code/google"

    val WHITE_LIST = listOf(
        AUTH_LOGIN,
        AUTH_REGISTER,
        AUTH_REFRESH,
        READYZ,
        "$BASE/me",
    )
    val GOOGLE_WHITE_LIST = listOf(
        "$GOOGLE_OAUTH2/**",
        "$GOOGLE_CALLBACK/**",
    )
}
