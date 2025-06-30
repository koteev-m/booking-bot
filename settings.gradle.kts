plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "booking_bot"
include("bot-gateway", "booking-api", "libs")