// build.gradle.kts (root)

plugins {
    kotlin("jvm") apply false
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

