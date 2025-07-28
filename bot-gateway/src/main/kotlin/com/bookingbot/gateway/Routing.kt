package com.bookingbot.gateway

import com.bookingbot.api.DatabaseFactory
import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.api.services.BookingService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import com.bookingbot.gateway.Role
import com.bookingbot.gateway.authorize
import org.koin.ktor.ext.inject

/**
 * Configure HTTP routes.
 */
fun Application.configureRouting() {
    install(ContentNegotiation) { json() }
    DatabaseFactory.init()
    routing {
        val service: BookingService by inject()
        get("/health") { call.respondText("OK") }

        post("/booking") {
            val dto = call.receive<BookingRequest>()
            call.respond(service.createBooking(dto))
        }

        // /bookings — RBA (Security)
        authorize(Role.ADMIN, Role.USER) {
            get("/bookings") {
                val all = service.getAllBookings()
                call.respond(HttpStatusCode.OK, all)
            }
            get("/bookings/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid booking ID")
                    return@get
                }
                service.getBooking(id)?.let { booking ->
                    call.respond(HttpStatusCode.OK, booking)
                } ?: call.respond(HttpStatusCode.NotFound)
            }
        }

        authorize(Role.ADMIN) {
            post("/bookings") {
                val req = call.receive<BookingRequest>()
                val created = service.createBooking(req)
                call.respond(HttpStatusCode.Created, created)
            }
            put("/bookings/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid booking ID")
                    return@put
                }
                val req = call.receive<BookingRequest>()
                if (service.updateBooking(id, req)) {
                    service.getBooking(id)?.let { updatedBooking ->
                        call.respond(HttpStatusCode.OK, updatedBooking)
                    } ?: call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            delete("/bookings/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid booking ID")
                    return@delete
                }
                if (service.deleteBooking(id)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
    startTelegramBot()
    environment.monitor.subscribe(ApplicationStopped) { ApplicationScope.cancel() }
}
