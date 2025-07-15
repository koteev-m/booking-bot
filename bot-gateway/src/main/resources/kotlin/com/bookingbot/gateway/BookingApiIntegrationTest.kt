// bot-gateway/src/test/kotlin/com/bookingbot/gateway/BookingApiIntegrationTest.kt

package com.bookingbot.gateway

import com.bookingbot.api.BookingRequest
import com.bookingbot.api.Booking
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import io.ktor.http.*
import kotlin.test.*

class BookingApiIntegrationTest {

    @Test
    fun `full CRUD lifecycle via HTTP`() = testApplication {
        // Подключаем модуль приложения
        application {
            module()
        }

        // Клиент с JSON support
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        // 1) Create
        val createReq = BookingRequest(userId = 42, tableId = 99, at = 1_234_567L)
        val createResponse = client.post("/bookings") {
            contentType(ContentType.Application.Json)
            setBody(createReq)
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val created: Booking = createResponse.body()
        assertEquals(createReq.userId, created.userId)
        assertEquals(createReq.tableId, created.tableId)
        assertEquals(createReq.at, created.at)

        // 2) Read single
        val getResponse = client.get("/bookings/${created.id}")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        val fetched: Booking = getResponse.body()
        assertEquals(created, fetched)

        // 3) Read all
        val listResponse = client.get("/bookings")
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val all: List<Booking> = listResponse.body()
        assertTrue(all.contains(created))

        // 4) Update
        val updateReq = BookingRequest(userId = 7, tableId = 8, at = 9L)
        val updateResponse = client.put("/bookings/${created.id}") {
            contentType(ContentType.Application.Json)
            setBody(updateReq)
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)

        val updatedFetched: Booking = client.get("/bookings/${created.id}").body()
        assertEquals(created.id, updatedFetched.id)
        assertEquals(updateReq.userId, updatedFetched.userId)
        assertEquals(updateReq.tableId, updatedFetched.tableId)
        assertEquals(updateReq.at, updatedFetched.at)

        // 5) Delete
        val deleteResponse = client.delete("/bookings/${created.id}")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // 6) Verify deletion
        val notFoundResponse = client.get("/bookings/${created.id}")
        assertEquals(HttpStatusCode.NotFound, notFoundResponse.status)
    }
}