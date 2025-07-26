package com.bookingbot.api.model.waitlist

import java.time.Instant

/** Entry in the waiting list. */
data class WaitEntry(
    val id: Int,
    val chatId: Long,
    val desiredTime: Instant,
    val preferredTable: Int?,
    val status: String,
    val createdAt: Instant
)
