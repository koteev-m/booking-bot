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
    // Версии из gradle.properties
    val exposedVersion: String   by rootProject.extra
    val flywayVersion: String    by rootProject.extra
    val postgresVersion: String  by rootProject.extra

    // --- Exposed BOM + модули Exposed ---
    // Отличный подход — использовать BOM
    implementation(platform("org.jetbrains.exposed:exposed-bom:$exposedVersion"))
    implementation("org.jetbrains.exposed:exposed-core")
    implementation("org.jetbrains.exposed:exposed-dao")
    implementation("org.jetbrains.exposed:exposed-jdbc")
    implementation("org.jetbrains.exposed:exposed-java-time")

    // Flyway + Postgres
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    runtimeOnly("org.postgresql:postgresql:$postgresVersion") // Драйвер БД лучше подключать как runtimeOnly

    // HikariCP и конфигурация
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("com.typesafe:config:1.4.3")

    // dotenv
    implementation("io.github.cdimascio:dotenv-kotlin:6.3.1")

    // --- Тесты ---
    // Всё правильно, тестовые зависимости с `testImplementation`
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${rootProject.extra["kotlinVersion"]}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("com.h2database:h2:2.1.214") // H2 только для тестов
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