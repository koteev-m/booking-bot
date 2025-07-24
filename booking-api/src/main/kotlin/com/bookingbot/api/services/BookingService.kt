package com.bookingbot.api.services

import com.bookingbot.api.model.booking.Booking
import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.api.tables.BookingsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction

class BookingService {

    /**
     * Создает новую бронь в базе данных.
     * @param request Объект с данными для создания брони.
     * @return Созданный объект Booking.
     */
    fun createBooking(request: BookingRequest): Booking = transaction {
        val id = BookingsTable.insertAndGetId {
            it[userId] = request.userId
            it[clubId] = request.clubId
            it[tableId] = request.tableId
            it[bookingTime] = request.bookingTime
            it[partySize] = request.partySize
            it[expectedDuration] = request.expectedDuration
            it[bookingGuestName] = request.bookingGuestName
            it[telegramId] = request.telegramId
            it[phone] = request.phone
            it[promoterId] = request.promoterId
            it[sourceName] = request.source
        }.value
        findBookingById(id) ?: throw IllegalStateException("Failed to create or find booking with id $id")
    }

    /**
     * Находит все бронирования для конкретного пользователя.
     * @param requestingUserId ID пользователя в Telegram.
     * @return Список объектов Booking.
     */
    fun findBookingsByUserId(requestingUserId: Long): List<Booking> = transaction {
        BookingsTable
            .select { BookingsTable.userId eq requestingUserId }
            .map { it.toBooking() }
    }

    /** Возвращает все бронирования. */
    fun getAllBookings(): List<Booking> = transaction {
        BookingsTable.selectAll().map { it.toBooking() }
    }

    /** Возвращает конкретное бронирование по ID. */
    fun getBooking(id: Int): Booking? = findBookingById(id)

    /** Обновляет данные бронирования. */
    fun updateBooking(id: Int, request: BookingRequest): Boolean = transaction {
        BookingsTable.update({ BookingsTable.id eq id }) {
            it[userId] = request.userId
            it[clubId] = request.clubId
            it[tableId] = request.tableId
            it[bookingTime] = request.bookingTime
            it[partySize] = request.partySize
            it[expectedDuration] = request.expectedDuration
            it[bookingGuestName] = request.bookingGuestName
            it[telegramId] = request.telegramId
            it[phone] = request.phone
            it[promoterId] = request.promoterId
            it[sourceName] = request.source
        } > 0
    }

    /** Удаляет бронирование. */
    fun deleteBooking(id: Int): Boolean = transaction {
        BookingsTable.deleteWhere { BookingsTable.id eq id } > 0
    }

    /**
     * Находит бронирование по его уникальному ID.
     * @param id ID бронирования.
     * @return Объект Booking или null, если не найден.
     */
    fun findBookingById(id: Int): Booking? = transaction {
        BookingsTable.select { BookingsTable.id eq id }.map { it.toBooking() }.singleOrNull()
    }

    /**
     * Отменяет бронирование, изменяя его статус на 'CANCELLED'.
     * Проверяет, что отменить бронь может только тот пользователь, который ее создал.
     * @param bookingId ID бронирования для отмены.
     * @param requestingUserId ID пользователя, который пытается отменить бронь.
     * @return true, если бронь была успешно найдена и отменена, иначе false.
     */
    fun cancelBooking(bookingId: Int, requestingUserId: Long): Boolean = transaction {
        BookingsTable.update({ (BookingsTable.id eq bookingId) and (BookingsTable.userId eq requestingUserId) }) {
            it[status] = "CANCELLED"
        } > 0
    }

    /**
     * Отменяет бронирование от имени персонала (админа/владельца), не проверяя ID пользователя.
     * @return true, если бронь была успешно найдена и отменена, иначе false.
     */
    fun cancelBookingByStaff(bookingId: Int): Boolean = transaction {
        BookingsTable.update({ BookingsTable.id eq bookingId }) {
            it[status] = "CANCELLED"
        } > 0
    }

    /**
     * Находит все бронирования, созданные конкретным промоутером.
     */
    fun findBookingsByPromoterId(promoterId: Long): List<Booking> = transaction {
        BookingsTable
            .select { BookingsTable.promoterId eq promoterId }
            .orderBy(BookingsTable.bookingTime, SortOrder.DESC)
            .map { it.toBooking() }
    }

    fun updateBookingStatus(bookingId: Int, newStatus: String): Boolean = transaction {
        BookingsTable.update({ BookingsTable.id eq bookingId }) {
            it[status] = newStatus
        } > 0
    }

    /**
     * Находит все активные бронирования для конкретного клуба.
     */
    fun findActiveBookingsByClub(clubId: Int): List<Booking> = transaction {
        BookingsTable
            .select { (BookingsTable.clubId eq clubId) and (BookingsTable.status inList listOf("PENDING", "SEATED")) }
            .orderBy(BookingsTable.bookingTime, SortOrder.ASC)
            .map { it.toBooking() }
    }

    /**
     * Приватная функция-расширение для конвертации строки из базы данных в объект Booking.
     */
    private fun ResultRow.toBooking(): Booking = Booking(
        id = this[BookingsTable.id].value,
        userId = this[BookingsTable.userId],
        clubId = this[BookingsTable.clubId],
        tableId = this[BookingsTable.tableId],
        bookingTime = this[BookingsTable.bookingTime],
        partySize = this[BookingsTable.partySize],
        expectedDuration = this[BookingsTable.expectedDuration],
        bookingGuestName = this[BookingsTable.bookingGuestName],
        telegramId = this[BookingsTable.telegramId],
        phone = this[BookingsTable.phone],
        status = this[BookingsTable.status],
        createdAt = this[BookingsTable.createdAt],
        promoterId = this[BookingsTable.promoterId],
        source = this[BookingsTable.sourceName]
    )
}

