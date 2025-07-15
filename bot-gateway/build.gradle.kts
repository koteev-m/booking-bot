// bot-gateway/build.gradle.kts
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
    application
}

// group и version наследуются из корневого проекта

application {
    mainClass.set("com.bookingbot.gateway.ApplicationKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// repositories не нужен, наследуется из settings.gradle.kts

dependencies {
    implementation(project(":booking-api"))

    // --- Ktor BOM + основные артефакты ---
    implementation(platform("io.ktor:ktor-bom:${rootProject.extra["ktorVersion"]}"))
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")

    implementation("com.github.kotlin-telegram-bot:kotlin-telegram-bot:${rootProject.extra["telegramBotVersion"]}")
    implementation("org.slf4j:slf4j-simple:2.0.7")

    // --- Coroutines BOM + core ---
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:${rootProject.extra["coroutinesVersion"]}"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // H2 только для тестов приложения
    testImplementation("com.h2database:h2:2.1.214")

    // --- Тестовые зависимости ---
    testImplementation("io.ktor:ktor-server-test-host")

    // The rest of the test dependencies are correct.
    testImplementation("io.ktor:ktor-client-cio")
    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${rootProject.extra["kotlinVersion"]}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("com.h2database:h2:2.1.214")
}

// Настройка компилятора Kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        // jvmTarget не нужен, так как он наследуется из java.toolchain
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.test {
    useJUnitPlatform()
}

// Отличная практика для избежания проблем при сборке дистрибутива
tasks.named<Tar>("distTar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.named<org.gradle.api.tasks.bundling.Zip>("distZip") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}