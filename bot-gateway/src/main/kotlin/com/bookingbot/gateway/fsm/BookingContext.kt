package com.bookingbot.gateway.fsm

import java.time.Instant

data class BookingContext(
    var clubId: Int? = null,
    var bookingDate: Instant? = null,
    var tableId: Int? = null,
    var guestCount: Int? = null,
    var phone: String? = null,
    var bookingGuestName: String? = null,
    var promoterId: Long? = null, // <<< Убедитесь, что это поле есть
    var source: String? = null
)
