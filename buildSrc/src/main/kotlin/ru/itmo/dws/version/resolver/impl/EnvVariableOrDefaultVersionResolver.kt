package ru.itmo.dws.version.resolver.impl

import ru.itmo.dws.version.resolver.VersionResolver

class EnvVariableOrDefaultVersionResolver(
    private val variable: String,
    private val fallback: String,
) : VersionResolver {
    override fun createNextVersion(): String {
        return System.getenv(variable)?.toString() ?: fallback
    }
}
