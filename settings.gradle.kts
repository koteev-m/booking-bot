//  ──────────────────────────────────────────────────────────────────────────────
//  Настройка репозиториев для плагинов и зависимостей на уровне settings
//  ──────────────────────────────────────────────────────────────────────────────

pluginManagement {
    repositories {
        gradlePluginPortal()    // для плагинов (kotlin, ktor и т.д.)
        mavenCentral()          // сюда Gradle будет загружать транзитивы плагинов (Jib, Jackson…)
    }
    plugins {
        kotlin("jvm") version "1.9.10"
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
        id("io.ktor.plugin") version "3.2.1"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()          // все библиотеки
        maven("https://jitpack.io")
    }
}

rootProject.name = "booking_bot"
include("booking-api", "bot-gateway", "libs")