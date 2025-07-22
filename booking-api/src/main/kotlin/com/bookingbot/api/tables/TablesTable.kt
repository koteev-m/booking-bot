package com.bookingbot.api.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import java.math.BigDecimal

object TablesTable : IntIdTable("tables") {
    val clubId = integer("club_id").references(ClubsTable.id)
    val tableNumber = integer("table_number")
    val capacity = integer("capacity")
    val minDeposit = decimal("min_deposit", 10, 2)
}
