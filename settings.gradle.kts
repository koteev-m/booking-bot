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
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
        id("io.ktor.plugin") version ktorVersion

    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // Сначала ищем в стандартном репозитории
        mavenCentral()
        // Затем в JitPack для специфичных библиотек
        maven { url = uri("https://jitpack.io") }
    }
    versionCatalogs {
        create("libs")
    }
}

rootProject.name = "booking_bot"
include("booking-api", "bot-gateway", "bot-gateway-e2e", "libs")
