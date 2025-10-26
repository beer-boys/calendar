package ru.itmo.dws.version.resolver.impl

import ru.itmo.dws.version.resolver.VersionResolver

class ConstantVersionResolver(private val version: String) : VersionResolver {
    override fun createNextVersion(): String = version
}
