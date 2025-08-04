<<<<<<< HEAD
#!/usr/bin/env bash
set -e
./gradlew clean build --refresh-dependencies --info
=======
#!/bin/bash

# Цвета для вывода
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}▶️ Шаг 1: Применяем исправления к файлам Gradle...${NC}"

# === Исправляем settings.gradle.kts ===
# Эта конфигурация правильно разделяет репозитории
cat > settings.gradle.kts << EOL
pluginManagement {
    val props = java.util.Properties().apply {
        file("gradle.properties").inputStream().use { load(it) }
    }
    val kotlinVersion = props.getProperty("kotlinVersion")
    val ktorVersion  = props.getProperty("ktorVersion")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
        id("io.ktor.plugin") version ktorVersion
        id("com.github.johnrengelman.shadow") version "8.1.1"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // Сначала ищем в стандартном репозитории
        mavenCentral()
        // Затем в JitPack для специфичных библиотек
        maven { url = uri("https://jitpack.io") }
    }
    versionCatalogs {
        create("libs")
    }
}

rootProject.name = "booking_bot"
include("booking-api", "bot-gateway", "bot-gateway-e2e", "libs")
EOL

# === Исправляем gradle/libs.versions.toml ===
# Здесь исправлены все имена библиотек на их актуальные версии
cat > gradle/libs.versions.toml << EOL
[versions]
kotlin = "1.9.23"
ktor = "2.3.8"
telegram = "6.3.0"
coroutines = "1.7.3"
exposed = "0.50.0"
flyway = "10.14.0"
postgres = "42.7.3"
hikaricp = "5.0.1"
config = "1.4.3"
dotenv = "6.3.1"
logback = "1.4.11"
koin = "3.5.6"
jwt = "4.4.0"
h2 = "2.1.214"
junit = "5.10.0"
mockito = "5.9.0"
mockitoKotlin = "5.2.1"
wiremock = "3.3.1"
lettuce = "6.3.2.RELEASE"
caffeine = "3.1.8"
serializationJson = "1.6.3"
jedis = "5.2.0"
embeddedRedis = "0.7.3"
backoff = "0.4.0"

[libraries]
exposed-bom = { module = "org.jetbrains.exposed:exposed-bom", version.ref = "exposed" }
exposed-core = { module = "org.jetbrains.exposed:exposed-core" }
exposed-dao = { module = "org.jetbrains.exposed:exposed-dao" }
exposed-jdbc = { module = "org.jetbrains.exposed:exposed-jdbc" }
exposed-java-time = { module = "org.jetbrains.exposed:exposed-java-time" }
flyway-core = { module = "org.flywaydb:flyway-core", version.ref = "flyway" }
postgresql = { module = "org.postgresql:postgresql", version.ref = "postgres" }
hikaricp = { module = "com.zaxxer:HikariCP", version.ref = "hikaricp" }
typesafe-config = { module = "com.typesafe:config", version.ref = "config" }
dotenv-kotlin = { module = "io.github.cdimascio:dotenv-kotlin", version.ref = "dotenv" }
ktor-server-core = { module = "io.ktor:ktor-server-core", version.ref = "ktor" }
ktor-server-content-negotiation = { module = "io.ktor:ktor-server-content-negotiation", version.ref = "ktor" }
ktor-server-auth = { module = "io.ktor:ktor-server-auth", version.ref = "ktor" }
kotlin-test-junit5 = { module = "org.jetbrains.kotlin:kotlin-test-junit5", version.ref = "kotlin" }
junit-jupiter-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit" }
junit-jupiter-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }
mockito-inline = { module = "org.mockito:mockito-inline", version.ref = "mockito" }
mockito-kotlin = { module = "org.mockito.kotlin:mockito-kotlin", version.ref = "mockitoKotlin" }
h2 = { module = "com.h2database:h2", version.ref = "h2" }
java-jwt = { module = "com.auth0:java-jwt", version.ref = "jwt" }
koin-ktor = { module = "io.insert-koin:koin-ktor", version.ref = "koin" }
koin-test = { module = "io.insert-koin:koin-test", version.ref = "koin" }
logback-classic = { module = "ch.qos.logback:logback-classic", version.ref = "logback" }
coroutines-bom = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-bom", version.ref = "coroutines" }
coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
ktor-bom = { module = "io.ktor:ktor-bom", version.ref = "ktor" }
ktor-server-core-jvm = { module = "io.ktor:ktor-server-core-jvm", version.ref = "ktor" }
ktor-server-netty-jvm = { module = "io.ktor:ktor-server-netty-jvm", version.ref = "ktor" }
ktor-server-content-negotiation-jvm = { module = "io.ktor:ktor-server-content-negotiation-jvm", version.ref = "ktor" }
ktor-serialization-kotlinx-json-jvm = { module = "io.ktor:ktor-serialization-kotlinx-json-jvm", version.ref = "ktor" }
ktor-server-auth-jvm = { module = "io.ktor:ktor-server-auth-jvm", version.ref = "ktor" }
ktor-server-auth-jwt-jvm = { module = "io.ktor:ktor-server-auth-jwt-jvm", version.ref = "ktor" }
ktor-server-auth-jwt = { module = "io.ktor:ktor-server-auth-jwt", version.ref = "ktor" }
ktor-server-test-host-jvm = { module = "io.ktor:ktor-server-test-host-jvm", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serializationJson" }
jedis = { module = "redis.clients:jedis", version.ref = "jedis" }
embedded-redis = { module = "it.ozimov:embedded-redis", version.ref = "embeddedRedis" }
ktor-core = { module = "io.ktor:ktor-server-core", version.ref = "ktor" }
ktor-metrics = { module = "io.ktor:ktor-server-metrics-micrometer", version.ref = "ktor" }
koin-logger = { module = "io.insert-koin:koin-logger-slf4j", version.ref = "koin" }
lettuce = { module = "io.lettuce:lettuce-core", version.ref = "lettuce" }
caffeine = { module = "com.github.ben-manes.caffeine:caffeine", version.ref = "caffeine" }

# --- Исправленные координаты ---
telegram-bot = { module = "com.github.kotlin-telegram-bot:kotlin-telegram-bot", version.ref = "telegram" }
wiremock = { module = "org.wiremock:wiremock-jre8-standalone", version.ref = "wiremock" }
backoff = { module = "io.github.reugn:kotlin-backoff-coroutines", version.ref = "backoff" }
EOL

echo -e "${GREEN}✅ Файлы Gradle успешно обновлены.${NC}"
echo -e "${GREEN}▶️ Шаг 2: Полная очистка и сборка проекта...${NC}"
echo "Это может занять несколько минут."

# Принудительная очистка и сборка
./gradlew --stop
sudo rm -rf ~/.gradle
rm -rf .gradle build
./gradlew build --refresh-dependencies

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅✅✅ СБОРКА УСПЕШНО ЗАВЕРШЕНА! ✅✅✅${NC}"
    echo "Теперь, пожалуйста, перезагрузите ваш проект в IDE (Reload All Gradle Projects)."
else
    echo -e "${RED}❌ Сборка завершилась с ошибкой. Пожалуйста, проверьте лог выше.${NC}"
fi
>>>>>>> 884cda7 (add ch)
