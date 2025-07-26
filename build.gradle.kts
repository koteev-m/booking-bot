// build.gradle.kts (root)

plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("io.ktor.plugin") apply false
    id("com.github.ben-manes.versions") version "0.51.0"
    id("org.owasp.dependencycheck") version "9.2.0"
}

tasks.named<org.owasp.dependencycheck.gradle.extension.DependencyCheckTask>("dependencyCheckAnalyze") {
    failBuildOnCVSS = 7.0F
    suppressionFile = "dependency-check-suppressions.xml"
}