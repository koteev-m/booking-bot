package com.bookingbot.api.tables

import org.jetbrains.exposed.sql.Table

object ClubStaffTable : Table("club_staff") {
    val userId = long("user_id").references(UsersTable.telegramId)
    val clubId = integer("club_id").references(ClubsTable.id)
    override val primaryKey = PrimaryKey(userId, clubId)
}
