import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    // Версию плагина берём из settings.gradle.kts → pluginManagement
    kotlin("jvm")
}

java {
    toolchain {
        // Жёстко фиксируем JDK 17 для сборки этого модуля
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Если в будущем появятся общие библиотеки,
    // которые вы хотите транзитивно подтягивать в booking-api и bot-gateway,
    // объявляйте их здесь через `api(...)`, а не `implementation(...)`:
    //
    // api("com.some:shared-lib:1.2.3")
    //
    // Тогда во всех зависимых модулях они будут доступны автоматически.
}