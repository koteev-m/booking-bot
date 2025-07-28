package com.bookingbot.gateway.di

import com.bookingbot.api.model.booking.Booking
import com.bookingbot.api.model.booking.BookingRequest

interface BookingService {
    fun createBooking(request: BookingRequest): Booking
    fun getFreeTables(): List<Int>
}
