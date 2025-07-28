package com.bookingbot.gateway.integration

import com.bookingbot.api.DatabaseFactory
import com.bookingbot.api.model.booking.BookingRequest
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import java.time.Instant
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals

class BookingApiIntegrationTest {
    @Test
    fun `create and fetch booking`() = testApplication {
        environment {
            systemProperties["DB_URL"] = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            systemProperties["BASIC_USER"] = "admin"
            systemProperties["BASIC_PASS"] = "pass"
            systemProperties["JWT_SECRET"] = "secret"
        }
        application { com.bookingbot.gateway.Application().module() }
        DatabaseFactory.init()
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        val req = BookingRequest(1,1,1,Instant.EPOCH,1,60,"G",123,"+1",null,"test")
        val auth = "Basic " + Base64.getEncoder().encodeToString("admin:pass".toByteArray())
        val resp = client.post("/bookings") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        assertEquals(HttpStatusCode.Created, resp.status)
        val created = resp.body<com.bookingbot.api.model.booking.Booking>()
        val get = client.get("/bookings/${'$'}{created.id}") { header(HttpHeaders.Authorization, auth) }
        assertEquals(HttpStatusCode.OK, get.status)
    }
}
