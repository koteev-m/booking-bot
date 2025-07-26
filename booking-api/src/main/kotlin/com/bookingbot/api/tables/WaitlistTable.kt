package com.bookingbot.api.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object WaitlistTable : IntIdTable("waiting_list") {
    val chatId = long("chat_id")
    val desiredTime = timestamp("desired_time")
    val preferredTable = integer("preferred_table").nullable()
    val status = varchar("status", 20).default("ACTIVE")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}
