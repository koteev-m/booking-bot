package com.bookingbot.api

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseFactoryTest {

    @Test
    fun `database factory correctly reports table existence`() {
        DatabaseFactory.init() // Инициализирует БД и создаёт таблицу Bookings для H2

        assertTrue(DatabaseFactory.exists("bookings"), "Таблица 'bookings' должна существовать после init()")
        assertFalse(DatabaseFactory.exists("non_existent_table"), "Таблица 'non_existent_table' не должна существовать")
    }
}