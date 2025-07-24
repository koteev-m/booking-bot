package com.bookingbot.api

import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.api.services.BookingService
import com.bookingbot.api.tables.BookingsTable
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*
import java.time.Instant
import java.time.temporal.ChronoUnit

class BookingServiceTest {
    private val service = BookingService()

    @BeforeTest
    fun setup() {
        DatabaseFactory.init()
        transaction { BookingsTable.deleteAll() }
    }

    @AfterTest
    fun teardown() {
        transaction { BookingsTable.deleteAll() }
    }

    @Test
    fun `create and retrieve booking`() {
        val ts = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val request = BookingRequest(
            userId = 1L,
            clubId = 42,
            tableId = 2,
            bookingTime = ts,
            partySize = 4,
            expectedDuration = 90,
            bookingGuestName = "Alice",
            telegramId = 123456789L,
            phone = "+7-999-123-45-67",
            promoterId = null,
            source = "test"
        )

        val created = service.createBooking(request)
        val fetched = service.getBooking(created.id)
        assertEquals(created, fetched)
    }
}
