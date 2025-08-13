plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
