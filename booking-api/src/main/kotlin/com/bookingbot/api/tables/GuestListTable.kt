package com.bookingbot.api.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object GuestListTable : IntIdTable("guest_list") {
    val clubId = integer("club_id").references(ClubsTable.id)
    val promoterId = long("promoter_id").references(UsersTable.telegramId)
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val checkedIn = bool("checked_in").default(false)
}
