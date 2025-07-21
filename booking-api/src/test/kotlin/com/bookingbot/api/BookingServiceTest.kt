package com.bookingbot.api

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.deleteAll

class BookingServiceTest {
    private val service = BookingService()

    @BeforeTest
    fun setup() {
        // Инициализация БД в памяти (H2) для каждого теста
        DatabaseFactory.init()
        // Очищаем таблицу перед каждым тестом для изоляции
        transaction {
            Bookings.deleteAll()
        }
    }

    @AfterTest
    fun teardown() {
        // Очищаем таблицу и после каждого теста
        transaction {
            Bookings.deleteAll()
        }
    }

    @Test
    fun `create and retrieve booking`() {
        val ts = Instant.now().truncatedTo(ChronoUnit.SECONDS) // Используем фиксированное время
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

        val timeBeforeCreation = Instant.now()
        val created = service.createBooking(request)

        // 1. Проверяем, что ID был сгенерирован
        assertTrue(created.id > 0, "Generated id should be positive")

        // 2. Проверяем, что все поля соответствуют запросу
        assertEquals(request.userId,           created.userId)
        assertEquals(request.clubId,           created.clubId)
        assertEquals(request.tableId,          created.tableId)
        assertEquals(request.bookingTime,      created.bookingTime)
        assertEquals(request.partySize,        created.partySize)
        assertEquals(request.expectedDuration, created.expectedDuration)
        assertEquals(request.guestName,        created.guestName)
        assertEquals(request.telegramId,       created.telegramId)
        assertEquals(request.phone,            created.phone)

        // 3. Проверяем поля со значениями по умолчанию
        assertEquals("PENDING", created.status)
        assertNotNull(created.createdAt)

        // 4. Проверяем, что 'createdAt' находится в адекватном диапазоне
        val timeAfterCreation = Instant.now()
        assertTrue(
            created.createdAt >= timeBeforeCreation.minusSeconds(1) && created.createdAt <= timeAfterCreation.plusSeconds(1),
            "createdAt should be very close to the moment of creation. Was: ${created.createdAt}"
        )

        // 5. Проверяем получение созданной записи
        val fetched = service.getBooking(created.id)
        assertEquals(created, fetched)

        // 6. Проверяем, что запись есть в общем списке
        val all = service.getAllBookings()
        assertEquals(1, all.size)
        assertEquals(created, all.first())
    }

    @Test
    fun `update booking`() {
        val original = service.createBooking(
            BookingRequest(
                userId = 1, clubId = 42, tableId = 2,
                bookingTime = Instant.ofEpochMilli(1_000L),
                partySize = 4, guestName = "Alice"
            )
        )

        val updatedReq = BookingRequest(
            userId = 10, clubId = 99, tableId = 3,
            bookingTime = Instant.ofEpochMilli(2_000L),
            partySize = 2, expectedDuration = 120,
            guestName = "Bob", telegramId = 987654321L,
            phone = "+7-888-765-43-21"
        )

        val wasUpdated = service.updateBooking(original.id, updatedReq)
        assertTrue(wasUpdated, "updateBooking should return true for an existing ID")

        val fetched = service.getBooking(original.id)
        assertNotNull(fetched)

        // Проверяем, что обновились все поля из запроса
        assertEquals(original.id,               fetched.id) // ID не меняется
        assertEquals(updatedReq.userId,         fetched.userId)
        assertEquals(updatedReq.clubId,         fetched.clubId)
        assertEquals(updatedReq.tableId,        fetched.tableId)
        assertEquals(updatedReq.bookingTime,    fetched.bookingTime)
        assertEquals(updatedReq.partySize,      fetched.partySize)
        assertEquals(updatedReq.expectedDuration, fetched.expectedDuration)
        assertEquals(updatedReq.guestName,      fetched.guestName)
        assertEquals(updatedReq.telegramId,     fetched.telegramId)
        assertEquals(updatedReq.phone,          fetched.phone)
        assertEquals(original.createdAt,        fetched.createdAt) // createdAt не меняется
        assertEquals(original.status,           fetched.status) // status не меняется
    }

    @Test
    fun `delete booking`() {
        val created = service.createBooking(
            BookingRequest(
                userId = 1, clubId = 42, tableId = 2,
                bookingTime = Instant.ofEpochMilli(1_000L),
                partySize = 4
            )
        )

        val wasDeleted = service.deleteBooking(created.id)
        assertTrue(wasDeleted, "deleteBooking should return true for an existing ID")

        // После удаления getBooking возвращает null
        assertNull(service.getBooking(created.id))
        // И список всех бронирований пуст
        assertTrue(service.getAllBookings().isEmpty())
    }

    @Test
    fun `getBooking for non-existent id returns null`() {
        assertNull(service.getBooking(99999))
    }

    @Test
    fun `updateBooking for non-existent id returns false`() {
        val request = BookingRequest(
            userId = 1, clubId = 1, tableId = 1,
            bookingTime = Instant.now(), partySize = 1
        )
        val wasUpdated = service.updateBooking(99999, request)
        assert(!wasUpdated, { "updateBooking should return false for a non-existent ID" })
    }

    @Test
    fun `deleteBooking for non-existent id returns false`() {
        val wasDeleted = service.deleteBooking(99999)
        assert(!wasDeleted, { "deleteBooking should return false for a non-existent ID" })
    }
}