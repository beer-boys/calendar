plugins {
    alias(libs.plugins.kotlin.jvm)

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

dependencies {
    detektPlugins(gav(libs.detekt.formatting))

    implementation(libs.spring.starter.web)
    implementation(libs.spring.starter.data.jdbc)
    implementation(libs.spring.starter.oauth2.client)
    implementation(libs.spring.starter.actuator)

    implementation(libs.fasterxml.jackson.kotlin)
    implementation(libs.kotlin.reflect)

    implementation(libs.liquibase.core)

    implementation(libs.postgresql)
    implementation("com.google.api-client:google-api-client:2.8.1")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.39.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0")

    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
    implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
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
