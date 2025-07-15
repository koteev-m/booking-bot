import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")                         // версия берётся из settings.gradle.kts → pluginManagement
    id("org.jetbrains.kotlin.plugin.serialization")
    `java-library`
}

group = "com.bookingbot.api"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Берём версии из корня (gradle.properties или extra в settings.gradle.kts)
    val exposedVersion: String   by rootProject.extra
    val flywayVersion: String    by rootProject.extra
    val postgresVersion: String  by rootProject.extra

    // --- Exposed BOM + модули Exposed без явных версий ---
    implementation(platform("org.jetbrains.exposed:exposed-bom:$exposedVersion"))
    implementation("org.jetbrains.exposed:exposed-core")
    implementation("org.jetbrains.exposed:exposed-dao")
    implementation("org.jetbrains.exposed:exposed-jdbc")
    implementation("org.jetbrains.exposed:exposed-java-time")

    // Flyway + Postgres
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")

    // HikariCP и конфигурация
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("com.typesafe:config:1.4.3")

    // dotenv (удаляйте, если не нужен .env в этом модуле)
    implementation("io.github.cdimascio:dotenv-kotlin:6.3.1")

    // --- Тесты ---
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${rootProject.extra["kotlinVersion"]}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("com.h2database:h2:2.1.214")
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