package com.bookingbot.api.services

import com.bookingbot.api.tables.BookingsTable
import com.bookingbot.api.tables.TablesTable
import java.time.LocalDateTime
import java.time.ZoneId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Repository for booking related queries.
 */
object BookingRepository {

    /**
     * Returns availability map for all tables at a specific time.
     *
     * @param desiredTime desired booking time.
     * @return map of table id to `true` if table is free, `false` otherwise.
     */
    fun findFreeTables(desiredTime: LocalDateTime): Map<Int, Boolean> = transaction {
        val start = desiredTime.minusHours(2).atZone(ZoneId.systemDefault()).toInstant()
        val end = desiredTime.plusHours(2).atZone(ZoneId.systemDefault()).toInstant()

        val cnt = BookingsTable.id.count()
        // Переменная tableIdColumn больше не нужна

        val availability: Map<Int, Boolean> = TablesTable
            .join(
                BookingsTable,
                JoinType.LEFT,
                // ПРАВИЛЬНО: Указываем условие соединения здесь:
                onColumn = TablesTable.id,
                otherColumn = BookingsTable.tableId,
                additionalConstraint = {
                    // Оставляем здесь только фильтры (время и статус):
                    (BookingsTable.bookingTime greaterEq start) and
                        (BookingsTable.bookingTime lessEq end) and
                        (BookingsTable.status inList listOf("PENDING", "SEATED"))
                }
            )
            // Используем TablesTable.id напрямую
            .slice(TablesTable.id, cnt)
            .selectAll()
            .groupBy(TablesTable.id)
            .associate { row ->
                val tableId: Int = row[TablesTable.id].value
                tableId to (row[cnt] == 0L)
            }
        availability
    }
}

