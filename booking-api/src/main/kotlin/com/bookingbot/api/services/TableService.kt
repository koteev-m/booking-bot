package com.bookingbot.api.services

import com.bookingbot.api.model.booking.Table
import java.time.Instant

class TableService {
    /**
     * Заглушка для получения списка свободных столов.
     * В реальном приложении здесь будет сложный запрос к БД.
     */
    fun getAvailableTables(clubId: Int, date: Instant, guestCount: Int): List<Table> {
        // TODO: Реализовать реальную логику проверки занятости столов
        return listOf(
            Table(id = 1, number = 1, capacity = 4),
            Table(id = 2, number = 2, capacity = 4),
            Table(id = 5, number = 5, capacity = 6),
            Table(id = 8, number = 8, capacity = 8)
        ).filter { it.capacity >= guestCount }
    }
}
