package com.bookingbot.api

import org.jetbrains.exposed.sql.ResultRow

/** Единственное расширение для конвертации строки в DTO */
fun ResultRow.toBooking(): Booking = Booking(
    id               = this[Bookings.id].value,
    userId           = this[Bookings.userId],
    clubId           = this[Bookings.clubId],
    tableId          = this[Bookings.tableId],
    bookingTime      = this[Bookings.bookingTime],
    partySize        = this[Bookings.partySize],
    expectedDuration = this[Bookings.expectedDuration],
    guestName        = this[Bookings.guestName],
    telegramId       = this[Bookings.telegramId],
    phone            = this[Bookings.phone],
    status           = this[Bookings.status],
    createdAt        = this[Bookings.createdAt]
)