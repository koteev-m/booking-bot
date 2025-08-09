package com.bookingbot.api.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object ClubsTable : IntIdTable("clubs") {
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val adminChannelId = long("admin_channel_id").nullable()
    val hasGuestList = bool("has_guest_list").default(false)
    val maxGuestListSize = integer("max_guest_list_size").nullable()
}