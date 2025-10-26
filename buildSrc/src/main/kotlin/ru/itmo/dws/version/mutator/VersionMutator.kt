package ru.itmo.dws.version.mutator

interface VersionMutator {
    fun mutateVersion(version: String): String = version
}
