package com.bookingbot.api.services

import com.bookingbot.api.model.booking.Table
import com.bookingbot.api.tables.BookingsTable
import com.bookingbot.api.tables.TablesTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class TableService {

    /**
     * Получает список свободных столов на всю "рабочую ночь".
     * "Рабочая ночь" определяется как период с 23:00 выбранной даты до 12:00 следующей.
     */
    fun getAvailableTables(clubId: Int, bookingTime: Instant, guestCount: Int): List<Table> = transaction {
        // 1. Находим все столы в клубе, подходящие по вместимости
        val potentialTables = TablesTable
            .select { (TablesTable.clubId eq clubId) and (TablesTable.capacity greaterEq guestCount) }
            .map { it.toTable() }

        if (potentialTables.isEmpty()) {
            return@transaction emptyList()
        }

        // 2. Определяем начало и конец "рабочей ночи"
        val localBookingDate = bookingTime.atZone(ZoneId.systemDefault()).toLocalDate()

        // Ночь начинается в 23:00 выбранной даты
        val operationalNightStart = localBookingDate.atTime(23, 0).atZone(ZoneId.systemDefault()).toInstant()
        // Ночь заканчивается в 12:00 следующего дня
        val operationalNightEnd = localBookingDate.plusDays(1).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()

        // 3. Находим ID всех столов, которые уже забронированы на эту ночь
        val bookedTableIds: Set<Int> = BookingsTable
            .select {
                (BookingsTable.clubId eq clubId) and
                        (BookingsTable.status inList listOf("PENDING", "SEATED")) and
                        // Ищем любую бронь в пределах этой "рабочей ночи"
                        (BookingsTable.bookingTime greaterEq operationalNightStart) and
                        (BookingsTable.bookingTime less operationalNightEnd)
            }
            .map { it[BookingsTable.tableId] }
            .toSet()

        // 4. Возвращаем только те столы, которых нет в списке занятых
        potentialTables.filterNot { it.id in bookedTableIds }
    }

    /**
     * Получает все столы для конкретного клуба.
     */
    fun getTablesForClub(clubId: Int): List<Table> = transaction {
        TablesTable.select { TablesTable.clubId eq clubId }.map { it.toTable() }
    }

    /**
     * Создает новый стол для клуба.
     */
    fun createTable(clubId: Int, number: Int, capacity: Int, deposit: BigDecimal): Table = transaction {
        val id = TablesTable.insertAndGetId {
            it[TablesTable.clubId] = clubId
            it[tableNumber] = number
            it[TablesTable.capacity] = capacity
            it[minDeposit] = deposit
        }
        // Находим и возвращаем созданный стол
        TablesTable.select { TablesTable.id eq id }.map { it.toTable() }.single()
    }

    /**
     * Обновляет вместимость стола.
     */
    fun updateTableCapacity(tableId: Int, newCapacity: Int): Boolean = transaction {
        TablesTable.update({ TablesTable.id eq tableId }) {
            it[capacity] = newCapacity
        } > 0
    }

    /**
     * Обновляет минимальный депозит стола.
     */
    fun updateTableDeposit(tableId: Int, newDeposit: BigDecimal): Boolean = transaction {
        TablesTable.update({ TablesTable.id eq tableId }) {
            it[minDeposit] = newDeposit
        } > 0
    }

    /**
     * Calculates deposit amount for given table and guest count.
     */
    fun calculateDeposit(tableId: Int, guestCount: Int): BigDecimal = transaction {
        val depositPerGuest: BigDecimal = TablesTable
            .select { TablesTable.id eq tableId }
            .map { it[TablesTable.minDeposit] }
            .singleOrNull() ?: BigDecimal.ZERO
        depositPerGuest.multiply(BigDecimal(guestCount))
    }

    /**
     * Приватная функция-расширение для конвертации строки из базы данных (ResultRow) в объект Table.
     */
    private fun ResultRow.toTable(): Table = Table(
        id = this[TablesTable.id].value,
        clubId = this[TablesTable.clubId],
        number = this[TablesTable.tableNumber],
        capacity = this[TablesTable.capacity],
        minDeposit = this[TablesTable.minDeposit]
    )
}