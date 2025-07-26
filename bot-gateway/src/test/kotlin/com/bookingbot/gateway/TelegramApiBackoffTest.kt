package com.bookingbot.gateway

import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelegramApiBackoffTest {
    @Test
    fun `retries with backoff`() = runTest {
        var attempts = 0
        val start = System.currentTimeMillis()
        val result = TelegramApi.withBackoffRetry {
            attempts++
            if (attempts <= 3) throw IOException("boom")
            "ok"
        }
        val elapsed = System.currentTimeMillis() - start
        assertEquals("ok", result)
        assertEquals(4, attempts)
        val expectedMin = BackoffConfig.load().initialDelayMs
        assertTrue(elapsed >= expectedMin)
    }
}
