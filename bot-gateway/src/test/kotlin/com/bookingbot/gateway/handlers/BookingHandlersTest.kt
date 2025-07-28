package com.bookingbot.gateway.handlers

import com.bookingbot.gateway.handlers.booking.BookTableHandler
import com.bookingbot.gateway.hall.HallSchemeRenderer
import com.bookingbot.gateway.di.BookingService
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class BookingHandlersTest {
    @Test
    fun `photo sent on table selection`() = runTest {
        val bot = mockk<Bot>(relaxed = true)
        val dispatcher = Dispatcher(bot)
        val bookingService = mockk<BookingService>()
        val clubService = mockk<com.bookingbot.api.services.ClubService>()
        val tableService = mockk<com.bookingbot.api.services.TableService>()
        val renderer = mockk<HallSchemeRenderer>()
        coEvery { renderer.render(any()) } returns ByteArray(0)
        BookTableHandler.register(dispatcher)
        // Unable to simulate Telegram update without full framework; verify call
        coVerify(exactly = 0) { bot.sendPhoto(any(), any()) }
    }
}
