// build.gradle.kts (root)

plugins {
    // Declare only non-core plugins for subprojects.
    // 'application' and 'java-library' are core plugins and don't need to be here.
    kotlin("jvm") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("io.ktor.plugin") apply false
}

// Share versions from gradle.properties with all subprojects using `extra`.
subprojects {
    val kotlinVersion: String by project
    extra["kotlinVersion"] = kotlinVersion

    val ktorVersion: String by project
    extra["ktorVersion"] = ktorVersion

    val telegramBotVersion: String by project
    extra["telegramBotVersion"] = telegramBotVersion

    val coroutinesVersion: String by project
    extra["coroutinesVersion"] = coroutinesVersion

    val exposedVersion: String by project
    extra["exposedVersion"] = exposedVersion

    val flywayVersion: String by project
    extra["flywayVersion"] = flywayVersion

    val postgresVersion: String by project
    extra["postgresVersion"] = postgresVersion
}

// Common configuration for all modules
allprojects {
    group = "com.bookingbot"
    version = "0.1.0-SNAPSHOT"
}

