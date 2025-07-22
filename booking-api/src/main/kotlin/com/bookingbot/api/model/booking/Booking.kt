package com.bookingbot.api.model.booking

import java.time.Instant

data class Booking(
    val id: Int,
    val userId: Long,
    val clubId: Int,
    val tableId: Int,
    val bookingTime: Instant,
    val partySize: Int,
    val expectedDuration: Int?,
    val bookingGuestName: String?,
    val telegramId: Long?,
    val phone: String?,
    val status: String,
    val createdAt: Instant,
    val promoterId: Long?,
    val source: String
)