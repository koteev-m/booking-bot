import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    `java-library`
}

group   = "com.bookingbot"
version = "1.0.0"

/* ---------- toolchain ---------- */
java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
kotlin.jvmToolchain(21)
/* ------------------------------- */

val kotlinVersion: String by project
val ktorVersion: String by project
val micrometerVersion: String by project

/* ---------- версии из gradle.properties ---------- */
val exposedVersion:   String by project
val postgresVersion:  String by project
val flywayVersion:    String by project
/* ------------------------------------------------- */

dependencies {
    implementation("io.ktor:ktor-server-core:${ktorVersion}")
    implementation("io.ktor:ktor-server-metrics-micrometer:${ktorVersion}")
// Micrometer + Prometheus
    implementation("io.micrometer:micrometer-registry-prometheus:${micrometerVersion}")
    // Exposed ORM (одна версия)
    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    api("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // DB & util
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.typesafe:config:1.4.3")
    implementation("io.github.cdimascio:dotenv-kotlin:6.3.1")

    // tests
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("com.h2database:h2:2.1.214")
}

tasks.test { useJUnitPlatform() }
