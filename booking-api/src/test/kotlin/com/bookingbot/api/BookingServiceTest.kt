package com.bookingbot.api

import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.api.services.BookingService
import com.bookingbot.api.tables.BookingsTable
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.*

class BookingServiceTest : KoinTest {
    private val service: BookingService by inject()
    private val testModule = module {
        single { DatabaseFactory }
        single { BookingService() }
    }

    @BeforeTest
    fun setup() {
        startKoin { modules(testModule) }
        DatabaseFactory.init()
        transaction { BookingsTable.deleteAll() }
    }

    @AfterTest
    fun teardown() {
        transaction { BookingsTable.deleteAll() }
        stopKoin()
    }

    @Test
    fun createAndRetrieveBooking() {
        val ts = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val request = BookingRequest(
            userId = 1,
            clubId = 42,
            tableId = 2,
            bookingTime = ts,
            partySize = 4,
            expectedDuration = 90,
            bookingGuestName = "Alice",
            telegramId = 123456789L,
            phone = "+7-999-123-45-67",
            bookingSource = "Бот"
        )

        val created = service.createBooking(request)
        val fetched = service.getBooking(created.id)

        assertNotNull(fetched)
        assertEquals(request.userId, fetched.userId)
        assertEquals(request.clubId, fetched.clubId)
    }

    @Test
    fun updateBooking() {
        val original = service.createBooking(
            BookingRequest(
                userId = 1,
                clubId = 42,
                tableId = 2,
                bookingTime = Instant.ofEpochMilli(1_000L),
                partySize = 4,
                expectedDuration = 120,
                bookingGuestName = "Alice",
                telegramId = null,
                phone = null,
                bookingSource = "Бот"
            )
        )

        val updatedReq = BookingRequest(
            userId = 10,
            clubId = 99,
            tableId = 3,
            bookingTime = Instant.ofEpochMilli(2_000L),
            partySize = 2,
            expectedDuration = 120,
            bookingGuestName = "Bob",
            telegramId = 987654321L,
            phone = "+7-888-765-43-21",
            bookingSource = "Бот"
        )

        val updated = service.updateBooking(original.id, updatedReq)
        assertTrue(updated)

        val fetched = service.getBooking(original.id)
        assertNotNull(fetched)
        assertEquals(updatedReq.clubId, fetched.clubId)
        assertEquals(updatedReq.bookingGuestName, fetched.bookingGuestName)
    }

    @Test
    fun deleteBooking() {
        val created = service.createBooking(
            BookingRequest(
                userId = 1,
                clubId = 42,
                tableId = 2,
                bookingTime = Instant.ofEpochMilli(1_000L),
                partySize = 4,
                expectedDuration = 120,
                bookingGuestName = null,
                telegramId = null,
                phone = null,
                bookingSource = "Бот"
            )
        )

        val deleted = service.deleteBooking(created.id)
        assertTrue(deleted)
        assertNull(service.getBooking(created.id))
    }
}
