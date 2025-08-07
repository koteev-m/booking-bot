import org.gradle.jvm.toolchain.JavaLanguageVersion
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar   // ← нужный import

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")

    /*  ✓ id остаётся прежним!
        settings.gradle.kts уже содержит ResolutionStrategy,
        который перенаправит этот id на артефакт com.gradleup.shadow:shadow-gradle-plugin:9.0.0  */
    id("com.github.johnrengelman.shadow")
}

group   = "com.bookingbot"
version = "1.0.0"

/* ---------- toolchain ---------- */
java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
kotlin.jvmToolchain(21)
/* ------------------------------- */

/* ---------- версии из gradle.properties ---------- */
val ktorVersion:        String by project
val telegramBotVersion: String by project   // 6.1.7
val micrometerVersion:  String by project
val koinVersion:        String by project
/* ------------------------------------------------- */

dependencies {
    /* ---- Ktor ---- */
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")

    /* ---- Micrometer / Prometheus ---- */
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

    /* ---- Telegram Bot API ---- */
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:$telegramBotVersion")

    /* ---- DI (Koin) ---- */
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    /* ---- Logging ---- */
    implementation("ch.qos.logback:logback-classic:1.4.7")

    /* ---- Tests ---- */
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
}

tasks.test { useJUnitPlatform() }

/* ---------- Shadow-jar ---------- */
tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("all")      // итоговый файл bot-gateway-all.jar
}
