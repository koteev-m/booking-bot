// settings.gradle.kts

// The plugins block is evaluated early and has a restricted scope.
// We cannot access outside variables directly.
// We must read the properties inside or pass them in a specific way.
pluginManagement {
    // Reading properties reliably for the plugins block
    val props = java.util.Properties()
    val propertiesFile = file("gradle.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { props.load(it) }
    } else {
        // It's better to fail early if the properties file is missing
        throw GradleException("Could not find gradle.properties in root directory")
    }

    // It's crucial to ensure the property is not null to avoid ambiguity
    val kotlinVersion = props.getProperty("kotlinVersion")
        ?: throw GradleException("kotlinVersion not found in gradle.properties")
    val ktorVersion = props.getProperty("ktorVersion")
        ?: throw GradleException("ktorVersion not found in gradle.properties")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
        id("io.ktor.plugin") version ktorVersion
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        // Add the Ktor EAP repository to resolve version 3.x artifacts
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    }
}

rootProject.name = "booking_bot"
include("booking-api", "bot-gateway", "libs")