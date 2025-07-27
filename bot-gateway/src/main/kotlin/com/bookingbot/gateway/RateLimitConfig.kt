package com.bookingbot.gateway

import com.typesafe.config.ConfigFactory
import java.time.Duration

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
                window = cfg.getDuration("window"),
                requests = cfg.getInt("requests")
            )
        }
    }
}
