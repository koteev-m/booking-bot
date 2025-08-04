pluginManagement {
<<<<<<< HEAD
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
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
=======
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
>>>>>>> 884cda7 (add ch)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
<<<<<<< HEAD
    versionCatalogs {
        create("libs")
    }
}
rootProject.name = "booking_bot"
include("booking-api", "bot-gateway", "bot-gateway-e2e", "libs")
=======
}

rootProject.name = "booking-bot"
include("booking-api")
include("bot-gateway")
include("bot-gateway-e2e")
include("libs")
include("admin-panel")
>>>>>>> 884cda7 (add ch)
