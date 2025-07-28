// build.gradle.kts (root)

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("io.ktor.plugin") apply false
    id("com.github.ben-manes.versions") version "0.51.0"
    id("org.owasp.dependencycheck") version "9.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

dependencyCheck {
    failBuildOnCVSS = 7.0F
    suppressionFiles = listOf("dependency-check-suppressions.xml")
}

dependencies {
    // --- unit ---
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("io.kotest:kotest-assertions-core:5.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // --- integration ---
    testImplementation("io.ktor:ktor-server-test-host:2.3.+")
    testImplementation("org.testcontainers:postgresql:1.19.1")
    testImplementation("com.h2database:h2:2.2.224")

    // --- E2E ---
    testImplementation("com.github.tomakehurst:wiremock-jre8:3.3.1")
}

tasks.test { useJUnitPlatform() }

