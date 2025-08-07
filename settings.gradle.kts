pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.github.johnrengelman.shadow") {
                // Redirect *any* request for the old id to the new GradleUp 9.0.0 plugin
                useModule("com.gradleup.shadow:shadow-gradle-plugin:9.0.0")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "booking-bot"
include("booking-api", "bot-gateway")
