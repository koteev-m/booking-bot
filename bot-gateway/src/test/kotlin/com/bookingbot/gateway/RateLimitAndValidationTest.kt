package com.bookingbot.gateway

import com.bookingbot.api.model.booking.BookingRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import java.time.Instant

class RateLimitAndValidationTest : StringSpec({
    "testRateLimitExceeded" {
        testApplication {
            application { module() }
            val client = createClient { install(ContentNegotiation) { json() } }
            val req = BookingRequest(1,1,1,Instant.EPOCH,1,bookingGuestName="G",telegramId=1,phone="+12345678901",bookingSource="test")
            repeat(60) {
                client.post("/booking") {
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }
            }
            val resp = client.post("/booking") {
                contentType(ContentType.Application.Json)
                setBody(req)
            }
            resp.status shouldBe HttpStatusCode.TooManyRequests
        }
    }

    "testInvalidPhoneReturns400" {
        testApplication {
            application { module() }
            val client = createClient { install(ContentNegotiation) { json() } }
            val req = BookingRequest(1,1,1,Instant.EPOCH,1,bookingGuestName="G",telegramId=1,phone="bad",bookingSource="test")
            val resp = client.post("/booking") {
                contentType(ContentType.Application.Json)
                setBody(req)
            }
            resp.status shouldBe HttpStatusCode.BadRequest
        }
    }
})
