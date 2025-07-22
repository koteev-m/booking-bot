package com.bookingbot.api.model

import java.time.Instant

data class Event(
    val id: Int,
    val clubId: Int,
    val title: String,
    val description: String?,
    val eventDate: Instant,
    val imageUrl: String?
)
