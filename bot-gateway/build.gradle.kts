// bot-gateway/build.gradle.kts
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Tar
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
    application
}

application {
    mainClass.set("com.bookingbot.gateway.ApplicationKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Зависимость от вашего API-модуля
    implementation(project(":booking-api"))

    // --- Ktor BOM и основные модули ---
    val ktorVersion: String by rootProject.extra
    implementation(platform("io.ktor:ktor-bom:$ktorVersion"))
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")

    // --- Telegram Bot и логирование ---
    val telegramBotVersion: String by rootProject.extra
    implementation("com.github.kotlin-telegram-bot:kotlin-telegram-bot:$telegramBotVersion")
    implementation("org.slf4j:slf4j-simple:2.0.7")

    // --- Корутины ---
    val coroutinesVersion: String by rootProject.extra
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutinesVersion"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // --- Тестовые зависимости ---
    testImplementation(platform("io.ktor:ktor-bom:$ktorVersion"))
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("com.h2database:h2:2.2.224") // Обновленная версия H2
}

// Настройка компилятора Kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

// Настройка тестов
tasks.test {
    useJUnitPlatform()
}

// Избегаем дубликатов при сборке
tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.withType<org.gradle.api.tasks.bundling.Zip> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
