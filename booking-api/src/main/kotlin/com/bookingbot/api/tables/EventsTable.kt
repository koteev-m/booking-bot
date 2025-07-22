package com.bookingbot.api.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object EventsTable : IntIdTable("events") {
    val clubId = integer("club_id").references(ClubsTable.id)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val eventDate = timestamp("event_date")
    val imageUrl = varchar("image_url", 512).nullable()
    val createdAt = timestamp("created_at")
}
