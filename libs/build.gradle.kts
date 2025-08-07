import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // api("com.some:shared-lib:1.2.3")
}
