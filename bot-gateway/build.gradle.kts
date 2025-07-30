import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.tasks.bundling.Zip // <<< ИСПРАВЛЕНО: импортируем правильный класс Zip
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Tar

plugins {
    kotlin("jvm")                 // версия Kotlin берётся из settings.gradle.kts
    id("io.ktor.plugin")          // версия Ktor берётся из settings.gradle.kts
    id("com.github.johnrengelman.shadow") // Shadow-плагин версиями управляем в settings.gradle.kts
    id("application")             // Плагин application
}

group = "com.bookingbot"
version = "1.0.0"

application {
    mainClass.set("com.bookingbot.gateway.ApplicationKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

// Блок repositories здесь не нужен, так как он централизован в settings.gradle.kts

dependencies {
    implementation(project(":booking-api"))

    // Ktor
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.netty.jvm)
    implementation(libs.ktor.server.content.negotiation.jvm)
    implementation(libs.ktor.serialization.kotlinx.json.jvm)
    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.ktor.server.auth.jwt.jvm)
    implementation(libs.ktor.server.auth.jwt)
    implementation("io.ktor:ktor-server-rate-limit:3.2.2")
    implementation("io.ktor:ktor-server-request-validation:3.2.2")
    implementation(libs.java.jwt)
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.+")
    implementation("io.micrometer:micrometer-core:1.13.0")
    implementation(libs.ktor.metrics)

    // Koin
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger)
    testImplementation(libs.koin.test)

    // Telegram Bot
    implementation(libs.telegram.bot)

    // centralized logging backend
    implementation(libs.logback.classic)
    implementation(libs.lettuce)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.typesafe.config)
    implementation(libs.backoff)
    implementation(libs.caffeine)

    // В H2 нам нужен только для тестов в этом модуле
    testImplementation(libs.h2)
    testImplementation(libs.jedis)
    testImplementation(libs.embedded.redis)

    // Coroutines
    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.core)
    testImplementation(libs.coroutines.test)

    // Тестовый Ktor
    testImplementation(platform(libs.ktor.bom))
    testImplementation(libs.ktor.server.test.host.jvm)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)

    // JUnit5 + Kotlin Test
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test.junit5) // Явно указал версию для стабильности
    testImplementation(libs.ktor.client.mock)
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation(libs.coroutines.test)
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.9.0")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.9.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xjsr305=strict"
    }
}

tasks.test {
    useJUnitPlatform()
}

// Обычное имя без "-all" в classifier
tasks.named<org.gradle.jvm.tasks.Jar>("shadowJar") {
    archiveClassifier.set("")
}

// Исключаем дубли в дистрибутивах
tasks.named<Zip>("distZip") { // <<< ИСПРАВЛЕНО: используем правильный тип Zip
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.named<Tar>("distTar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
