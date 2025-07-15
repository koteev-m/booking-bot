package com.bookingbot.api

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import com.bookingbot.api.toBooking

class BookingService {

    /** Создаёт запись и сразу возвращает DTO, прочитав её из БД */
    fun createBooking(request: BookingRequest): Booking = transaction {
        val id = Bookings.insertAndGetId { row ->
            row[Bookings.userId]           = request.userId
            row[Bookings.clubId]           = request.clubId
            row[Bookings.tableId]          = request.tableId
            row[Bookings.bookingTime]      = request.bookingTime
            row[Bookings.partySize]        = request.partySize
            row[Bookings.expectedDuration] = request.expectedDuration
            row[Bookings.guestName]        = request.guestName
            row[Bookings.telegramId]       = request.telegramId
            row[Bookings.phone]            = request.phone
            // status и createdAt заполняются по умолчанию
        }.value

        Bookings
            .selectAll().where { Bookings.id eq id }
            .map(ResultRow::toBooking)
            .single()
    }

    fun getBooking(id: Int): Booking? = transaction {
        Bookings
            .selectAll().where { Bookings.id eq id }
            .map(ResultRow::toBooking)
            .singleOrNull()
    }

    fun getAllBookings(): List<Booking> = transaction {
        Bookings
            .selectAll()
            .map(ResultRow::toBooking)
    }

    fun updateBooking(id: Int, request: BookingRequest): Boolean = transaction {
        Bookings.update({ Bookings.id eq id }) { row ->
            row[Bookings.userId]           = request.userId
            row[Bookings.clubId]           = request.clubId
            row[Bookings.tableId]          = request.tableId
            row[Bookings.bookingTime]      = request.bookingTime
            row[Bookings.partySize]        = request.partySize
            row[Bookings.expectedDuration] = request.expectedDuration
            row[Bookings.guestName]        = request.guestName
            row[Bookings.telegramId]       = request.telegramId
            row[Bookings.phone]            = request.phone
            // не меняем status и createdAt
        } > 0
    }

    fun deleteBooking(id: Int): Boolean = transaction {
        Bookings.deleteWhere { Bookings.id eq id } > 0
    }
}