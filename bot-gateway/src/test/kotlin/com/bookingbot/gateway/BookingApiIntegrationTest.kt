package com.bookingbot.gateway

import com.bookingbot.api.model.booking.BookingRequest
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class BookingApiIntegrationTest {
    @Test
    fun shouldCreateBooking() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        val request = BookingRequest(
            userId = 1,
            clubId = 1,
            tableId = 1,
            bookingTime = Instant.EPOCH,
            partySize = 2,
            expectedDuration = 60,
            bookingGuestName = "Guest",
            telegramId = 123,
            phone = "+10000000000",
            promoterId = null,
            bookingSource = "test"
        )
        val response = client.post("/bookings") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }
}
