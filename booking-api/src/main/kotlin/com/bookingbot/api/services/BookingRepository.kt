package com.bookingbot.api.services

import com.bookingbot.api.tables.BookingsTable
import com.bookingbot.api.tables.TablesTable
import java.time.LocalDateTime
import java.time.ZoneId
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.join
import org.jetbrains.exposed.sql.selectAll
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

        TablesTable
            .join(
                BookingsTable,
                JoinType.LEFT,
                additionalConstraint = {
                    (BookingsTable.tableId eq TablesTable.id) and
                        (BookingsTable.bookingTime greaterEq start) and
                        (BookingsTable.bookingTime lessEq end) and
                        (BookingsTable.status inList listOf("PENDING", "SEATED"))
                }
            )
            .slice(TablesTable.id, cnt)
            .selectAll()
            .groupBy(TablesTable.id)
            .associate { it[TablesTable.id].value to (it[cnt] == 0L) }
    }
}

