import ru.itmo.dws.version.VersionConfig
import ru.itmo.dws.version.mutator.impl.LocalVersionMutator
import ru.itmo.dws.version.resolver.impl.ConstantVersionResolver
import java.net.URI
import java.util.UUID

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)

    `maven-publish`

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)

    alias(libs.plugins.detekt)
}

group = "ru.itmo.dws"
description = "calendar"

repositories {
    mavenCentral()
}

private fun envOrEmpty(env: String) = System.getenv(env)?.toString() ?: ""

// todo create version resolving
private val versionConfig = VersionConfig(
    resolver = ConstantVersionResolver("testing-${UUID.randomUUID()}"),
    mutators = mutableListOf(
        LocalVersionMutator(project.gradle.startParameter.taskNames),
    )
)

version = versionConfig.newVersion()

publishing {
    publications {
        create<MavenPublication>(name) {
            logger.info("create publication $groupId:$artifactId:$version")
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/beer-boys/calendar")
            credentials {
                username = envOrEmpty("GITHUB_ACTOR")
                password = envOrEmpty("GITHUB_TOKEN")
            }
        }
    }
}

dependencies {
    detektPlugins(gav(libs.detekt.formatting))

    implementation(libs.spring.starter.web)
    implementation(libs.spring.starter.data.jdbc)
    implementation(libs.spring.starter.oauth2.client)
    implementation(libs.spring.starter.actuator)
    implementation(libs.spring.starter.email)

    implementation(libs.fasterxml.jackson.kotlin)
    implementation(libs.kotlin.reflect)

    implementation(libs.postgresql)
    implementation(libs.liquibase.core)

    implementation("com.google.api-client:google-api-client:2.8.1")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.39.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0")

    implementation(libs.bundles.jjwt)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.reflect)

    testImplementation(libs.spring.starter.test)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)

    testImplementation(libs.mockk)
    testImplementation(libs.spring.mockk)
    testImplementation(libs.awaitility.kotlin)

    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgres)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    autoCorrect = true

    config.setFrom("detekt.yml")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

fun gav(provider: Provider<MinimalExternalModuleDependency>): String {
    return provider.get().toString()
}
