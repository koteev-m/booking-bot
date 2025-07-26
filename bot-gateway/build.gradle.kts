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

// подтягиваем переменные, которые объявлены в root gradle.properties
val ktorVersion: String by project
val coroutinesVersion: String by project

dependencies {
    implementation(project(":booking-api"))

    // Ktor
    implementation(platform("io.ktor:ktor-bom:$ktorVersion"))
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("com.auth0:java-jwt:4.4.0")

    // Koin
    implementation("io.insert-koin:koin-ktor:3.6.0")
    implementation("io.insert-koin:koin-logger-slf4j:3.6.0")
    testImplementation("io.insert-koin:koin-test:3.6.0")

    // Telegram Bot, без тянущихся артефактов Ktor
    implementation("io.github.kotlin-telegram-bot:kotlin-telegram-bot:6.3.0") {
        exclude(group = "io.ktor")
    }

    // centralized logging backend
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // В H2 нам нужен только для тестов в этом модуле
    testImplementation("com.h2database:h2:2.1.214")

    // Coroutines
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutinesVersion"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    // Тестовый Ktor
    testImplementation(platform("io.ktor:ktor-bom:$ktorVersion"))
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("io.ktor:ktor-client-cio")
    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json")

    // JUnit5 + Kotlin Test
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.23") // Явно указал версию для стабильности
    testImplementation("io.ktor:ktor-client-mock:2.3.+")
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
