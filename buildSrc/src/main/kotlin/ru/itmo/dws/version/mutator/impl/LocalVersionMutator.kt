package ru.itmo.dws.version.mutator.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.itmo.dws.version.mutator.VersionMutator

class LocalVersionMutator(private val tasks: Collection<String>) : VersionMutator {

    private val postfix = "_local"
    private val notEnglishRegex = Regex("[^A-Za-z]")

    override fun mutateVersion(version: String) = if (isLocalPush()) {
        (version + postfix).also {
            logger.info { "LocalMutator: Версия изменена с $version на $it" }
        }
    } else {
        version
    }

    private fun isLocalPush() = tasks.any {
        it.replace(notEnglishRegex, "").contains("publishToMavenLocal")
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
