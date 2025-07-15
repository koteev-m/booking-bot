package com.bookingbot.gateway

import com.bookingbot.api.Booking
import com.bookingbot.api.BookingRequest
import com.bookingbot.api.DatabaseFactory
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.test.*
import java.time.Instant
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ApplicationTest {

    @BeforeTest
    fun before() {
        // Инициализация in-memory БД
        DatabaseFactory.init()
    }

    @Test
    fun testCrudViaHttp() = testApplication {
        // Подставляем модуль напрямую, без запуска real server
        application { module() }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        // Создание
        val req = BookingRequest(
            userId = 1,
            clubId = 1,
            tableId = 2,
            bookingTime = Instant.ofEpochMilli(1_000L),
            partySize = 3,
            expectedDuration = 60,
            guestName = "TestUser",
            telegramId = 123L,
            phone = "+100"
        )
        val postResponse = client.post("/bookings") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        assertEquals(HttpStatusCode.OK, postResponse.status)
        val created = postResponse.body<Booking>()
        assertTrue(created.id > 0)

        // Чтение всех
        val list = client.get("/bookings").body<List<Booking>>()
        assertEquals(1, list.size)

        // Чтение по ID
        val getResponse = client.get("/bookings/${created.id}")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertEquals(created, getResponse.body<Booking>())

        // Обновление
        val updatedReq = req.copy(partySize = 5)
        val putResponse = client.put("/bookings/${created.id}") {
            contentType(ContentType.Application.Json)
            setBody(updatedReq)
        }
        assertEquals(HttpStatusCode.OK, putResponse.status)

        // Удаление
        val deleteResponse = client.delete("/bookings/${created.id}")
        assertEquals(HttpStatusCode.OK, deleteResponse.status)
    }
}