package com.bookingbot.api.model

import java.math.BigDecimal

/**
 * Simple DTO for promoter performance reports.
 */
data class PromoterStats(
    val promoterId: Long,
    val totalGuests: Int,
    val totalDeposit: BigDecimal
)
