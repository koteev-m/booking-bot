plugins {
    kotlin("jvm")
    `java-library`
}

group = "com.bookingbot.libs"
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
    // сюда будете складывать общие зависимости, например:
    // api("org.apache.commons:commons-text:1.10.0")
}