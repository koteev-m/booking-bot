// libs/build.gradle.kts
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
    `java-library` // Используем java-library для потенциального api/implementation
}

java {
    toolchain {
        // Отличная практика — жёстко фиксировать JDK для модуля
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Репозитории здесь не нужны, они наследуются из settings.gradle.kts

dependencies {
    // Если в будущем появятся общие библиотеки,
    // которые вы хотите транзитивно подтягивать в booking-api и bot-gateway,
    // объявляйте их здесь через `api(...)`, а не `implementation(...)`:
    //
    // api("com.some:shared-lib:1.2.3")
    //
    // Тогда во всех зависимых модулях они будут доступны автоматически.
}