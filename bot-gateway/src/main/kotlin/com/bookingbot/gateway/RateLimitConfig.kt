package com.bookingbot.gateway

import com.typesafe.config.ConfigFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Configuration for rate limiting.
 */
data class RateLimitConfig(
    val window: Duration,
    val requests: Int
) {
    companion object {
        fun load(): RateLimitConfig {
            val cfg = ConfigFactory.load().getConfig("security.rateLimit")
            return RateLimitConfig(
                window = cfg.getDuration("window").toMillis().milliseconds,
                requests = cfg.getInt("requests")
            )
        }
    }
}
