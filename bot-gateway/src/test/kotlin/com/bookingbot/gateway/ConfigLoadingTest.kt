package com.bookingbot.gateway

import com.typesafe.config.ConfigFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ConfigLoadingTest : StringSpec({
    "load custom bot config" {
        val config = ConfigFactory.parseResources("test.conf")
        val botCfg = config.getConfig("bot").toBotConfig()
        botCfg.waitlist.periodMs shouldBe 30000L
        botCfg.loyalty.pointsPerVisit shouldBe 25
    }
})
