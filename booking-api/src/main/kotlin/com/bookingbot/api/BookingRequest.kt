package com.bookingbot.api

import java.time.Instant

data class BookingRequest(
    val userId          : Int,
    val clubId          : Int,
    val tableId         : Int,
    val bookingTime     : Instant,
    val partySize       : Int,
    val expectedDuration: Int?     = null,
    val guestName       : String?  = null,
    val telegramId      : Long?    = null,
    val phone           : String?  = null
)