plugins {
    kotlin("jvm")                  apply false
    kotlin("plugin.serialization") apply false
    id("com.gradleup.shadow")      apply false
}

// общие настройки тестов
subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
