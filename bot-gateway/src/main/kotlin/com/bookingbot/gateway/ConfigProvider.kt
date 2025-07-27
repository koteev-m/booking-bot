package com.bookingbot.gateway

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/** Configuration data classes loaded from application.conf. */
data class WaitlistConfig(val periodMs: Long)
data class LoyaltyConfig(val pointsPerVisit: Int)
data class BotConfig(
    val waitlist: WaitlistConfig,
    val loyalty: LoyaltyConfig
)

/** Convert a [Config] subtree into [BotConfig]. */
fun Config.toBotConfig(): BotConfig = BotConfig(
    waitlist = getConfig("waitlist").let { WaitlistConfig(it.getLong("periodMs")) },
    loyalty = getConfig("loyalty").let { LoyaltyConfig(it.getInt("pointsPerVisit")) }
)

/** Provides configuration loaded from HOCON files. */
object ConfigProvider {
    val botConfig: BotConfig = ConfigFactory.load().getConfig("bot").toBotConfig()
}

