import java.net.URI

plugins {
    alias(libs.plugins.kotlin.jvm)

    `maven-publish`

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)

    alias(libs.plugins.detekt)
}

group = "ru.itmo.dws"
description = "calendar"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

private fun envOrEmpty(env: String) = System.getenv(env)?.toString() ?: ""

// todo create version resolving
publishing {
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
//    implementation(libs.spring.starter.data.jdbc)
    implementation(libs.spring.starter.actuator)

    implementation(libs.fasterxml.jackson.kotlin)
    implementation(libs.kotlin.reflect)

//    implementation(libs.liquibase.core)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.reflect)

    testImplementation(libs.spring.starter.test)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    autoCorrect = true

    config.setFrom("detekt.yml")
}

kotlin {
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
