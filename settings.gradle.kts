val kotlinVersion: String by settings
val ktorVersion: String by settings
val telegramBotVersion: String by settings

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        // Версию Kotlin-плагина берем из gradle.properties (kotlinVersion = "2.1.0")
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion

        // Версию Ktor-плагина тоже можно вынести в properties (ktorVersion = "3.2.1")
        id("io.ktor.plugin") version ktorVersion
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
include("booking-api", "bot-gateway", "libs")