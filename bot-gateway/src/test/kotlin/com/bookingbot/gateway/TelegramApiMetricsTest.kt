package com.bookingbot.gateway

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TelegramApiMetricsTest {
    @Test
    fun `metrics recorded`() = runTest {
        val registry = SimpleMeterRegistry()
        val orig = TelegramApi.meterRegistry
        TelegramApi.meterRegistry = registry

        TelegramApi.callTelegram("test") { "ok" }
        assertFailsWith<RuntimeException> {
            TelegramApi.callTelegram("test") { throw RuntimeException("boom") }
        }

        val success = registry.find("telegram_api_requests_total")
            .tags("method", "test", "status", "success")
            .counter()!!.count()
        val error = registry.find("telegram_api_requests_total")
            .tags("method", "test", "status", "error")
            .counter()!!.count()
        val timerCount = registry.find("telegram_api_latency_seconds")
            .tags("method", "test")
            .timer()!!.count()

        assertEquals(1.0, success)
        assertEquals(1.0, error)
        assertEquals(2, timerCount)
        TelegramApi.meterRegistry = orig
    }
}
