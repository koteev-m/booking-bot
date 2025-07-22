package com.bookingbot.gateway

import com.bookingbot.api.model.booking.`BookingRequest.kt`
import com.bookingbot.api.services.BookingService
import com.bookingbot.api.DatabaseFactory
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        // 👇👇👇 Явно указываем, какой модуль использовать, устраняя неоднозначность
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // Установка плагинов
    install(ContentNegotiation) {
        json()
    }

    // Инициализация БД
    DatabaseFactory.init()

    // Настройка роутинга
    routing {
        val bookingService = BookingService()
        route("/bookings") {
            post {
                val req = call.receive<`BookingRequest.kt`>()
                val created = bookingService.createBooking(req)
                call.respond(HttpStatusCode.Created, created)
            }
            get {
                val all = bookingService.getAllBookings()
                call.respond(HttpStatusCode.OK, all)
            }
            get("{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid booking ID")
                    return@get
                }
                bookingService.getBooking(id)?.let { booking ->
                    call.respond(HttpStatusCode.OK, booking)
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            put("{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid booking ID")
                    return@put
                }
                val req = call.receive<`BookingRequest.kt`>()
                if (bookingService.updateBooking(id, req)) {
                    bookingService.getBooking(id)?.let { updatedBooking ->
                        call.respond(HttpStatusCode.OK, updatedBooking)
                    } ?: call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            delete("{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid booking ID")
                    return@delete
                }
                if (bookingService.deleteBooking(id)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }

    // Запускаем бота вместе с сервером
    startTelegramBot()
}