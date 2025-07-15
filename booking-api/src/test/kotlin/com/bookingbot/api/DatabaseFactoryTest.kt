package com.bookingbot.api

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class DatabaseFactoryTest {

    @Test
    fun `database factory reports existing table`() {
        // Инициализируем БД и метаданные
        DatabaseFactory.init()

        // Проверяем наличие таблицы (stub-реализация всегда возвращает true)
        val tableExists = DatabaseFactory.exists("Bookings")
        assertTrue(tableExists, "Method exists should return true for any table name")
    }
}