import org.gradle.jvm.toolchain.JavaLanguageVersion
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar // класс остаётся из старого пакета

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.johnrengelman.shadow") // id старый, но резолвится в com.gradleup.shadow:9.0.0
}

group = "com.bookingbot"
version = "1.0.0"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
kotlin.jvmToolchain(21)

dependencies {
    // --- Ktor (сервер) ---
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.metrics) // Micrometer bridge

    // --- Micrometer / Prometheus ---
    implementation(libs.micrometer.registry.prometheus)

    // --- Telegram Bot (вариант A: JitPack 6.3.0) ---
    implementation(libs.telegram.bot)

    // --- DI (Koin) ---
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger)

    // --- Logging ---
    implementation(libs.logback.classic)

    // --- Tests ---
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.ktor.server.test.host)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("all") // bot-gateway-all.jar
}
