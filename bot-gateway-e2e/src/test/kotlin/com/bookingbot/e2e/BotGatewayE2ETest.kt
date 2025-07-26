package com.bookingbot.e2e

import com.bookingbot.gateway.Bot
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertTrue

class BotGatewayE2ETest {
    companion object {
        @JvmStatic
        @RegisterExtension
        val wireMock: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @BeforeAll
        @JvmStatic
        fun setup() {
            System.setProperty("TELEGRAM_BOT_TOKEN", "test")
            System.setProperty("TELEGRAM_API_URL", wireMock.baseUrl())
            Bot.instance.startPolling()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            Bot.instance.stopPolling()
        }
    }

    @Test
    fun `book table interaction`() = runBlocking {
        wireMock.stubFor(WireMock.post(WireMock.urlPathMatching("/bot.*/sendMessage"))
            .willReturn(WireMock.ok()))

        // Simulate user message. Library exposes function to process updates.
        Bot.instance.processUpdate("/book table 5", chatId = 1L)

        delay(500)
        assertTrue(wireMock.findAll(WireMock.postRequestedFor(WireMock.anyUrl())).isNotEmpty())
    }
}
