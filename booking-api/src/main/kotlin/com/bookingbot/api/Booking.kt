package com.bookingbot.api

import java.time.Instant

data class Booking(
    val id               : Int,
    val userId           : Int,
    val clubId           : Int,
    val tableId          : Int,
    val bookingTime      : Instant,
    val partySize        : Int,
    val expectedDuration : Int?,
    val guestName        : String?,
    val telegramId       : Long?,
    val phone            : String?,
    val status           : String,
    val createdAt        : Instant
)