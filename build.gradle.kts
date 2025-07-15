//  ──────────────────────────────────────────────────────────────────────────────
//  Здесь только «declare & defer» — версии плагинов, применяться будут в подпроектах
//  ──────────────────────────────────────────────────────────────────────────────

plugins {
    kotlin("jvm")                              apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("io.ktor.plugin")                       apply false
    application                                apply false
}

// опционально: общие свойства группы/версии
allprojects {
    group = "com.bookingbot"
    version = "0.1.0-SNAPSHOT"
}