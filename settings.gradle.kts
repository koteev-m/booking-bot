pluginManagement {
    val props = java.util.Properties().apply {
        file("gradle.properties").inputStream().use { load(it) }
    }
    val kotlinVersion = props.getProperty("kotlinVersion")
    val ktorVersion  = props.getProperty("ktorVersion")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm")                        version kotlinVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
        id("io.ktor.plugin")                 version ktorVersion
        id("com.github.johnrengelman.shadow") version "8.1.1"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
rootProject.name = "booking_bot"
// Bot module requires external dependencies from JitPack which are not
// accessible in the test environment. Excluding it from the build ensures
// tests can run without hitting the blocked repository.
include("booking-api", "libs")