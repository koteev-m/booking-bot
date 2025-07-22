package com.bookingbot.api.tables

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val telegramId = long("telegram_id").uniqueIndex()
    val username = varchar("username", 100).nullable()
    val role = varchar("role", 20).default("GUEST")
    val createdAt = org.jetbrains.exposed.sql.javatime.timestamp("created_at")
        .defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentTimestamp)

    override val primaryKey = PrimaryKey(telegramId)
}