package ru.itmo.dws.version.resolver

import java.io.Serializable

// Serializable for https://github.com/gradle/gradle/issues/16982
interface VersionResolver : Serializable {
    fun createNextVersion(): String
}
