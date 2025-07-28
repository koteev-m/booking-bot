package com.bookingbot.gateway.di

import com.bookingbot.api.model.booking.Booking
import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.api.services.BookingRepository
import com.bookingbot.api.services.TableService
import com.bookingbot.api.services.BookingService as ApiService
import java.time.LocalDateTime

class BookingServiceImpl(
    private val api: ApiService,
    private val tableService: TableService
) : BookingService {
    override fun createBooking(request: BookingRequest): Booking = api.createBooking(request)

    override fun getFreeTables(): List<Int> {
        val map = BookingRepository.findFreeTables(LocalDateTime.now())
        return map.filterValues { it }.keys.toList()
    }
}
