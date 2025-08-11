pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
    }
    // единая реализация Shadow 9.0.0 (новая группа com.gradleup.shadow)
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.github.johnrengelman.shadow") {
                useModule("com.gradleup.shadow:shadow-gradle-plugin:9.0.0")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        maven("https://jitpack.io") // нужен для Telegram 6.3.0 (JitPack)
    }
    // versionCatalogs НЕ трогаем — Gradle сам подхватывает gradle/libs.versions.toml
}

rootProject.name = "booking_bot"
include(
    "booking-api",
    "bot-gateway",
    "bot-gateway-e2e", // оставил, если у тебя есть модуль; можно удалить, если не нужен
    "libs"             // то же самое
)
