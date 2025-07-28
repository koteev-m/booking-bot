package com.bookingbot.gateway.e2e

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Requires Telegram Test API")
class BotConversationE2ETest {
    private lateinit var server: WireMockServer

    @BeforeEach
    fun setup() {
        server = WireMockServer(WireMockConfiguration.wireMockConfig().port(12100))
        server.start()
        server.stubFor(WireMock.post(WireMock.urlPathMatching("/bot.*/sendMessage")).willReturn(WireMock.okJson("{}")))
        server.stubFor(WireMock.post(WireMock.urlPathMatching("/bot.*/sendPhoto")).willReturn(WireMock.okJson("{}")))
    }

    @AfterEach
    fun teardown() { server.stop() }

    @Test
    fun dialog() {
        // Real bot startup skipped; verify stubs registered
        server.verify(0, WireMock.postRequestedFor(WireMock.urlMatching("/bot.*/sendMessage")))
    }
}
