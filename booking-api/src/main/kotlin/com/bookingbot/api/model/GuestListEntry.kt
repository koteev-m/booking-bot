package com.bookingbot.api.model

data class GuestListEntry(
    val id: Int,
    val clubId: Int,
    val promoterId: Long,
    val firstName: String,
    val lastName: String,
    val checkedIn: Boolean
)
