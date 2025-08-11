plugins {
    kotlin("jvm") version libs.versions.kotlin.get() apply false
    kotlin("plugin.serialization") version libs.versions.kotlin.get() apply false
    id("com.github.johnrengelman.shadow") apply false // редиректнётся на 9.0.0 через settings.gradle.kts
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
