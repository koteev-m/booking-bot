import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
}

group = "com.bookingbot"
version = "1.0.0"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
kotlin.jvmToolchain(21)

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

dependencies {
    // Exposed BOM и модули
    api(platform(libs.exposed.bom))
    api(libs.exposed.core)
    api(libs.exposed.dao)
    api(libs.exposed.jdbc)
    api(libs.exposed.java.time)

    // Database & config
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.typesafe.config)
    implementation(libs.dotenv.kotlin)

    // Kotlinx Serialization runtime (совместимо с Kotlin 2.2.x)
    implementation(libs.kotlinx.serialization.json)

    // Если модуль действительно использует Ktor (DTO/serializers/утилиты), оставьте блок ниже.
    // Если нет — можно удалить эти зависимости, чтобы уменьшить связность слоя данных.
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)

    // Тесты
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test.junit5) // ← добавлено
    testImplementation(libs.h2)
}
