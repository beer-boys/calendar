package ru.itmo.dws.version

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.itmo.dws.version.mutator.VersionMutator
import ru.itmo.dws.version.resolver.VersionResolver

class VersionConfig(
    var resolver: VersionResolver,
    var mutators: MutableList<VersionMutator>
) {

    fun newVersion(): String {
        val resolved = resolver.createNextVersion()
        var version = resolved
//        val tag = commitTag()
//        var version = if (tag != null) {
//            logger.info { "Found tag $tag on commit ${shortSha()}, use version from it" }
//            tag
//        } else {
//            logger.info { "Tag not found on commit ${shortSha()}, use version $resolved from resolver" }
//            resolved
//        }
        logger.info { "mutators : ${mutators.joinToString(", ") { it.javaClass.name }}" }
        mutators.forEach {
            version = it.mutateVersion(version)
        }
        logger.info { "version : $version" }
        return version
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
