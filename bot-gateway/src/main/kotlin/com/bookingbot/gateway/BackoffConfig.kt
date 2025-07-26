package com.bookingbot.gateway

import com.typesafe.config.ConfigFactory

/**
 * Configuration for exponential backoff retry logic.
 */
data class BackoffConfig(
    val initialDelayMs: Long,
    val maxDelayMs: Long,
    val factor: Double,
    val jitterPct: Double,
    val maxAttempts: Int
) {
    companion object {
        fun load(): BackoffConfig {
            val cfg = ConfigFactory.load().getConfig("telegram.retry")
            return BackoffConfig(
                initialDelayMs = cfg.getLong("initialDelayMs"),
                maxDelayMs = cfg.getLong("maxDelayMs"),
                factor = cfg.getDouble("factor"),
                jitterPct = cfg.getDouble("jitterPct"),
                maxAttempts = cfg.getInt("maxAttempts")
            )
        }
    }
}
