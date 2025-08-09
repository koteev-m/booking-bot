package com.bookingbot.api.model

data class Club(
    val id: Int,
    val name: String,
    val description: String,
    val adminChannelId: Long?,
    val hasGuestList: Boolean,
    val maxGuestListSize: Int?
)
