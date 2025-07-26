package com.bookingbot.api

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertThrows

class DatabaseFactoryTest {

    @Test
    fun `exists validates table name`() {
        DatabaseFactory.init() // Инициализирует БД и создаёт таблицу Bookings для H2

        assertTrue(DatabaseFactory.exists("bookings"), "Таблица 'bookings' должна существовать после init()")
        assertThrows<IllegalArgumentException> { DatabaseFactory.exists("non_existent_table") }
    }
}