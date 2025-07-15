// bot-gateway/src/test/kotlin/com/bookingbot/gateway/ApplicationTest.kt
package com.bookingbot.gateway

import com.bookingbot.api.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.time.Instant
import kotlin.test.*

class ApplicationTest {

    @BeforeTest
    fun before() {
        DatabaseFactory.init()
    }

    @Test
    fun testCrudViaHttp(): Unit = testApplication {
        application { module() }

        val created = client.post("/bookings") {
            contentType(ContentType.Application.Json)
            setBody(BookingRequest(
                userId = 1, clubId = 1, tableId = 2,
                bookingTime = Instant.ofEpochMilli(1_000L),
                partySize = 3, expectedDuration = 60,
                guestName = "TestUser", telegramId = 123L, phone = "+100"
            ))
        }.body<Booking>()

        assertTrue(created.id > 0)
        assertEquals("TestUser", created.guestName)

        val fetched = client.get("/bookings/${created.id}").body<Booking>()
        assertEquals(created, fetched)
    }
}