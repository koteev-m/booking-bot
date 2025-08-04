<<<<<<< HEAD
// build.gradle.kts (root)

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("io.ktor.plugin") apply false
    id("com.github.ben-manes.versions") version "0.51.0"
    id("org.owasp.dependencycheck") version "9.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
}


dependencyCheck {
    failBuildOnCVSS = 7.0F
    suppressionFile = "dependency-check-suppressions.xml"
}

dependencies {
    // --- unit ---
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("io.kotest:kotest-assertions-core:5.8.1")
    testImplementation(libs.coroutines.test)

    // --- integration ---
    testImplementation("io.ktor:ktor-server-test-host:2.3.+")
    testImplementation("org.testcontainers:postgresql:1.19.1")
    testImplementation("com.h2database:h2:2.2.224")

    // --- E2E ---
    testImplementation(libs.wiremock)
}

tasks.test { useJUnitPlatform() }

tasks.named("check") { dependsOn("dependencyCheckAnalyze") }

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.2.1")
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        }
        enableExperimentalRules.set(true)
        android.set(false)
        filter { exclude("**/generated/**") }
        additionalEditorconfig.set(mapOf("insert_final_newline" to "true"))
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.file("detekt.yml"))
        buildUponDefaultConfig = true
        allRules = false
        baseline = file("detekt-baseline.xml")
    }


    afterEvaluate {
        tasks.named("check") { dependsOn("ktlintCheck", "detekt") }
    }
}

=======
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.detekt)
    // Централизованно объявляем Shadow plugin
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
}

allprojects {
    group = "com.booking-bot"
    version = "1.0-SNAPSHOT"
}
>>>>>>> 884cda7 (add ch)
