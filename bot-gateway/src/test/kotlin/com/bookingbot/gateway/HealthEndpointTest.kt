package com.bookingbot.gateway

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.client.call.bodyAsText

class HealthEndpointTest : StringSpec({
    "health returns OK" {
        testApplication {
            application {
                configureRouting()
            }
            val resp = client.get("/health")
            resp.status shouldBe HttpStatusCode.OK
            resp.bodyAsText() shouldBe "OK"
        }
    }
})
