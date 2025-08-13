import org.gradle.jvm.toolchain.JavaLanguageVersion
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("application")
    alias(libs.plugins.shadow) // com.gradleup.shadow 9.x
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
    implementation(project(":booking-api"))

    // Ktor
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)

    // Serialization runtime (совместимо с Kotlin 2.2.x)
    implementation(libs.kotlinx.serialization.json)

    // Прочее
    implementation(libs.telegram.bot)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger)
    implementation(libs.logback.classic)

    // Тесты
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.ktor.server.test.host)
}

application {
    mainClass.set("com.bookingbot.gateway.ApplicationKt")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("all")
}
