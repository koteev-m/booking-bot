package com.bookingbot.api

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertThrows

class DatabaseFactoryTest {

    @Test
    fun `exists returns false for missing table`() {
        DatabaseFactory.init()
        assertFalse(DatabaseFactory.exists("tables"))
    }

    @Test
    fun `exists throws on illegal name`() {
        DatabaseFactory.init()
        assertThrows<IllegalArgumentException> {
            DatabaseFactory.exists("users; DROP TABLE bookings;")
        }
    }
}

