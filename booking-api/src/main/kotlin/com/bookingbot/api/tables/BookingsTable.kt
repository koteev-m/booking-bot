package com.bookingbot.api.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object BookingsTable : IntIdTable("bookings") {
    val userId = long("user_id")
    val clubId = integer("club_id")
    val tableId = integer("table_id")
    val bookingTime = timestamp("booking_time")
    val partySize = integer("party_size")
    val expectedDuration = integer("expected_duration").nullable()
    val bookingGuestName = varchar("booking_guest_name", 100).nullable()
    val telegramId = long("telegram_id").nullable()
    val phone = varchar("phone", 20).nullable()
    val status = varchar("status", 20).default("PENDING")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val promoterId = long("promoter_id").references(UsersTable.telegramId).nullable()
    val bookingSource = varchar("booking_source", 100).default("Бот")
}
