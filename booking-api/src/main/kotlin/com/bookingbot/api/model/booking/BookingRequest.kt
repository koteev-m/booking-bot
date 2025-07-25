package com.bookingbot.api.model.booking

import java.time.Instant

data class BookingRequest(
    val userId: Long,
    val clubId: Int,
    val tableId: Int,
    val bookingTime: Instant,
    val partySize: Int,
    val expectedDuration: Int? = 120,
    val bookingGuestName: String?,
    val telegramId: Long?,
    val phone: String?,
    val promoterId: Long? = null,
    val bookingSource: String
)
