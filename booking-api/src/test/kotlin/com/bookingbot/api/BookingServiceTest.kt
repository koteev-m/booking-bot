package com.bookingbot.api

import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.deleteAll

class BookingServiceTest {
    private val service = BookingService()

    @BeforeTest
    fun setup() {
        // Инициализация БД и миграций
        DatabaseFactory.init()
        // Очищаем таблицу перед каждым тестом
        transaction {
            Bookings.deleteAll()
        }
    }

    @AfterTest
    fun teardown() {
        // Очищаем после каждого теста
        transaction {
            Bookings.deleteAll()
        }
    }

    @Test
    fun `create and retrieve booking`() {
        val ts = Instant.ofEpochMilli(1_000L)
        val request = BookingRequest(
            userId           = 1,
            clubId           = 42,
            tableId          = 2,
            bookingTime      = ts,
            partySize        = 4,
            expectedDuration = 90,
            guestName        = "Alice",
            telegramId       = 123456789L,
            phone            = "+7-999-123-45-67"
        )
        val created = service.createBooking(request)

        // ID сгенерился
        assertTrue(created.id > 0, "Generated id should be positive")
        // все поля совпадают
        assertEquals(request.userId,           created.userId)
        assertEquals(request.clubId,           created.clubId)
        assertEquals(request.tableId,          created.tableId)
        assertEquals(request.bookingTime,      created.bookingTime)
        assertEquals(request.partySize,        created.partySize)
        assertEquals(request.expectedDuration, created.expectedDuration)
        assertEquals(request.guestName,        created.guestName)
        assertEquals(request.telegramId,       created.telegramId)
        assertEquals(request.phone,            created.phone)
        // статус по умолчанию из миграции
        assertEquals("PENDING", created.status)
        // createdAt должно быть близко к now, просто проверим, что не null
        assertTrue(created.createdAt.isBefore(Instant.now().plusSeconds(1)))

        // getBooking и getAllBookings
        val fetched = service.getBooking(created.id)
        assertEquals(created, fetched)
        val all = service.getAllBookings()
        assertEquals(1, all.size)
        assertEquals(created, all.first())
    }

    @Test
    fun `update booking`() {
        val original = service.createBooking(
            BookingRequest(
                userId           = 1,
                clubId           = 42,
                tableId          = 2,
                bookingTime      = Instant.ofEpochMilli(1_000L),
                partySize        = 4,
                expectedDuration = 90,
                guestName        = "Alice",
                telegramId       = 123456789L,
                phone            = "+7-999-123-45-67"
            )
        )

        val updatedReq = BookingRequest(
            userId           = 10,
            clubId           = 99,
            tableId          = 3,
            bookingTime      = Instant.ofEpochMilli(2_000L),
            partySize        = 2,
            expectedDuration = 120,
            guestName        = "Bob",
            telegramId       = 987654321L,
            phone            = "+7-888-765-43-21"
        )

        val updated = service.updateBooking(original.id, updatedReq)
        assertTrue(updated, "updateBooking должен вернуть true для существующего ID")

        val fetched = service.getBooking(original.id)!!
        assertEquals(original.id,               fetched.id)
        assertEquals(updatedReq.userId,         fetched.userId)
        assertEquals(updatedReq.clubId,         fetched.clubId)
        assertEquals(updatedReq.tableId,        fetched.tableId)
        assertEquals(updatedReq.bookingTime,    fetched.bookingTime)
        assertEquals(updatedReq.partySize,      fetched.partySize)
        assertEquals(updatedReq.expectedDuration, fetched.expectedDuration)
        assertEquals(updatedReq.guestName,      fetched.guestName)
        assertEquals(updatedReq.telegramId,     fetched.telegramId)
        assertEquals(updatedReq.phone,          fetched.phone)
    }

    @Test
    fun `delete booking`() {
        val created = service.createBooking(
            BookingRequest(
                userId           = 1,
                clubId           = 42,
                tableId          = 2,
                bookingTime      = Instant.ofEpochMilli(1_000L),
                partySize        = 4,
                expectedDuration = 90,
                guestName        = "Alice",
                telegramId       = 123456789L,
                phone            = "+7-999-123-45-67"
            )
        )

        val deleted = service.deleteBooking(created.id)
        assertTrue(deleted, "deleteBooking должен вернуть true для существующего ID")

        // После удаления getBooking возвращает null
        assertNull(service.getBooking(created.id))
        // И список пуст
        assertTrue(service.getAllBookings().isEmpty())
    }
}