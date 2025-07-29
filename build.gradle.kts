// build.gradle.kts (root)

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("io.ktor.plugin") apply false
    id("com.github.ben-manes.versions") version "0.51.0"
    id("org.owasp.dependencycheck") version "9.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
}


dependencyCheck {
    failBuildOnCVSS = 7.0F
    suppressionFile = "dependency-check-suppressions.xml"
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

