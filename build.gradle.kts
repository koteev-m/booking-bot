import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.shadow) apply false
}

subprojects {
    // Единообразная конфигурация Kotlin для всех JVM‑модулей (KGP 2.x)
    plugins.withId("org.jetbrains.kotlin.jvm") {
        the<KotlinJvmProjectExtension>().apply {
            jvmToolchain(21)
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
                // При необходимости можно держать строгую аннотационную политику JSR‑305
                freeCompilerArgs.addAll("-Xjsr305=strict")
            }
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
