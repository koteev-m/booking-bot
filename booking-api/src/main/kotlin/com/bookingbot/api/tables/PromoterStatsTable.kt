package com.bookingbot.api.tables

import java.math.BigDecimal
import org.jetbrains.exposed.dao.id.IntIdTable

object PromoterStatsTable : IntIdTable("promoter_stats") {
    val promoterId = long("promoter_id").uniqueIndex()
    val visits = integer("visits").default(0)
    val totalDeposit = decimal("total_deposit", 10, 2).default(BigDecimal.ZERO)
}

