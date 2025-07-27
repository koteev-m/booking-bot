package com.bookingbot.gateway

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import ktor_health_check.Health

class HealthModuleTest : StringSpec({
    "readyz returns 200 when checks pass" {
        testApplication {
            application {
                install(Health) {
                    readyCheck("db") { true }
                    readyCheck("redis") { true }
                }
            }
            val resp = client.get("/readyz")
            resp.status shouldBe HttpStatusCode.OK
        }
    }

    "readyz returns 503 when any check fails" {
        testApplication {
            application {
                install(Health) {
                    readyCheck("db") { true }
                    readyCheck("redis") { false }
                }
            }
            val resp = client.get("/readyz")
            resp.status shouldBe HttpStatusCode.ServiceUnavailable
        }
    }
})
