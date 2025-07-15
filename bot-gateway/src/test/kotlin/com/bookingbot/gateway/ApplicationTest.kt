package com.bookingbot.gateway

import com.bookingbot.api.BookingRequest
import com.bookingbot.api.BookingService
import com.bookingbot.api.DatabaseFactory
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.* // <-- Этот импорт исправляет ошибку 'Unresolved reference'
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // Инициализируем БД
    DatabaseFactory.init()

    // Настраиваем JSON
    install(ContentNegotiation) {
        json()
    }

    // Настраиваем роутинг
    routing {
        // Создаем один экземпляр сервиса, чтобы не создавать его при каждом запросе
        val bookingService = BookingService()

        route("/bookings") {
            // CREATE
            post {
                val req = call.receive<BookingRequest>()
                val created = bookingService.createBooking(req)
                call.respond(HttpStatusCode.Created, created)
            }
            // READ ALL
            get {
                val all = bookingService.getAllBookings()
                call.respond(HttpStatusCode.OK, all)
            }
            // READ ONE
            get("{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                // Проверяем, что ID корректный
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid booking ID format")
                    return@get
                }
                val booking = bookingService.getBooking(id)
                if (booking != null) {
                    call.respond(HttpStatusCode.OK, booking)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            // UPDATE
            put("{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid booking ID format")
                    return@put
                }
                val req = call.receive<BookingRequest>()
                if (bookingService.updateBooking(id, req)) {
                    // Возвращаем обновленный объект для ясности
                    val updatedBooking = bookingService.getBooking(id)
                    call.respond(HttpStatusCode.OK, updatedBooking!!)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            // DELETE
            delete("{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid booking ID format")
                    return@delete
                }
                if (bookingService.deleteBooking(id)) {
                    // Используем статус 204 No Content для успешного удаления
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
