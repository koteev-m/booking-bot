plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":bot-gateway"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.wiremock)
    testImplementation(libs.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
