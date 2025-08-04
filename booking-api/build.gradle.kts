<<<<<<< HEAD
// booking-api/build.gradle.kts
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization") // Для JSON
    `java-library` // Этот модуль — библиотека, поэтому `java-library` подходит лучше
}

// group и version теперь в корневом build.gradle.kts

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// repositories не нужен, наследуется из settings.gradle.kts

dependencies {
    // --- Exposed BOM + модули Exposed ---
    // Отличный подход — использовать BOM
    implementation(platform(libs.exposed.bom))
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)

    // Flyway + Postgres
    implementation(libs.flyway.core)
    runtimeOnly(libs.postgresql) // Драйвер БД лучше подключать как runtimeOnly

    // HikariCP и конфигурация
    implementation(libs.hikaricp)
    implementation(libs.typesafe.config)

    // dotenv
    implementation(libs.dotenv.kotlin)

    // Ktor server dependencies
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)

    // --- Тесты ---
    // Всё правильно, тестовые зависимости с `testImplementation`
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.h2) // H2 только для тестов
}

// Настройка компилятора Kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        // jvmTarget не нужен, он берётся из toolchain
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

// Настройка тестов
tasks.test {
    useJUnitPlatform()
}
=======
plugins {
    // Просто применяем плагины. Версии и конфигурация управляются централизованно.
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

application {
    mainClass.set("com.bookingbot.api.ApplicationKt")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.hikariCP)
    implementation(libs.flyway.core)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.mockk)
}
>>>>>>> 884cda7 (add ch)
