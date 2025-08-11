plugins {
    // версии заданы в settings.pluginManagement
    kotlin("jvm")                  apply false
    kotlin("plugin.serialization") apply false
    id("com.gradleup.shadow")      apply false
}

subprojects {
    // JUnit 5 для всех модулей
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
