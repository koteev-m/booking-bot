plugins {
<<<<<<< HEAD
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
=======
    id("java")
}

group = "com.bookingbot"
version = "1.0-SNAPSHOT"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
>>>>>>> 884cda7 (add ch)
    useJUnitPlatform()
}
