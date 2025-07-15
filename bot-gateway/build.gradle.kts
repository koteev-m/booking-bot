import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.file.DuplicatesStrategy

plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("com.bookingbot.gateway.ApplicationKt")
}

dependencies {
    implementation(project(":booking-api"))

    // Ktor BOM и основные артефакты
    implementation(platform("io.ktor:ktor-bom:3.2.1"))
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")

    implementation("com.github.kotlin-telegram-bot:kotlin-telegram-bot:6.3.0")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    implementation("com.h2database:h2:2.1.214")

    // Coroutines BOM + ядро
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.7.3"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    // Ktor-интеграционные тесты
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("io.ktor:ktor-client-cio")
    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json")

    // JUnit5 + Kotlin Test
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.10")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
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

// не дублировать артефакты в ZIP/TAR
tasks.named<Tar>("distTar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}