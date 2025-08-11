import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
}

group = "com.bookingbot"
version = "1.0.0"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
kotlin.jvmToolchain(21)

dependencies {
    // --- Exposed (одна версия через каталог) ---
    api(libs.exposed.core)
    api(libs.exposed.dao)
    api(libs.exposed.jdbc)
    api(libs.exposed.java.time)

    // --- DB & util ---
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.typesafe.config)
    implementation(libs.dotenv.kotlin)

    // --- Ktor для Metrics/Application utils ---
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.metrics) // Ktor ↔ Micrometer

    // --- Micrometer / Prometheus ---
    implementation(libs.micrometer.registry.prometheus)

    // --- Tests ---
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.h2)
}
