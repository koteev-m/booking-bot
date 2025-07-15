package com.bookingbot.api

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import java.time.Instant

object Bookings : IntIdTable("bookings") {
    val userId           : Column<Int>      = integer("user_id")
    val clubId           : Column<Int>      = integer("club_id")
    val tableId          : Column<Int>      = integer("table_id")
    val bookingTime      : Column<Instant>  = timestamp("booking_time")
    val partySize        : Column<Int>      = integer("party_size")
    val expectedDuration : Column<Int?>     = integer("expected_duration").nullable()
    val guestName        : Column<String?>  = varchar("guest_name", 100).nullable()
    val telegramId       : Column<Long?>    = long("telegram_id").nullable()
    val phone            : Column<String?>  = varchar("phone", 20).nullable()
    val status           : Column<String>   = varchar("status", 20).default("PENDING")
    val createdAt        : Column<Instant>  =
        timestamp("created_at")
            .defaultExpression(CurrentTimestamp)
}