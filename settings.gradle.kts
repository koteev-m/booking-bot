pluginManagement {
    resolutionStrategy {
        eachPlugin {
            // Страховка на случай, если где-то остался старый ID Shadow:
            // принудительно маппим com.github.johnrengelman.shadow -> новый модуль GradleUp Shadow 9.0.1
            if (requested.id.id == "com.github.johnrengelman.shadow") {
                useModule("com.gradleup.shadow:com.gradleup.shadow.gradle.plugin:9.0.1")
            }
        }
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // Централизуем репозитории: запрещаем объявлять их в проектах/модулях
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "booking-bot"
include("booking-api", "bot-gateway")
