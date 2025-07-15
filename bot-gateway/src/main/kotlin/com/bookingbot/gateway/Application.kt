package com.bookingbot.gateway

import com.bookingbot.api.DatabaseFactory
import com.bookingbot.api.BookingRequest
import com.bookingbot.api.BookingService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun main() {
    embeddedServer(
        factory = Netty,
        port    = 8080,
        module  = Application::module  // явно указываем наш модуль
    ).start(wait = true)
}

fun Application.module() {
    // Инициализируем БД и миграции
    DatabaseFactory.init()

    // JSON-плагин
    install(ContentNegotiation) {
        json()
    }

    // HTTP API
    routing {
        route("/bookings") {
            // CREATE
            post {
                val req = call.receive<BookingRequest>()
                val created = BookingService().createBooking(req)
                call.respond(HttpStatusCode.Created, created)
            }
            // READ ALL
            get {
                val all = BookingService().getAllBookings()
                call.respond(HttpStatusCode.OK, all)
            }
            // READ ONE
            get("{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                val booking = id?.let { BookingService().getBooking(it) }
                if (booking != null) {
                    call.respond(HttpStatusCode.OK, booking)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            // UPDATE
            put("{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                val req = call.receive<BookingRequest>()
                if (id != null && BookingService().updateBooking(id, req)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            // DELETE
            delete("{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id != null && BookingService().deleteBooking(id)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}