plugins {
    kotlin("jvm") version "2.2.0" apply false
    kotlin("plugin.serialization") version "1.9.24" apply false
    id("com.gradleup.shadow") version "9.0.0" apply false
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
