package com.bookingbot.gateway.unit

import com.bookingbot.api.DatabaseFactory
import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.WaitlistNotifier
import com.bookingbot.api.services.WaitlistNotifierHolder
import com.bookingbot.gateway.TelegramApi
import com.github.kotlintelegrambot.entities.ChatId
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertTrue

class BookingServiceTest {
    @Test
    fun `create booking sends telegram message`() = runTest {
        DatabaseFactory.init()
        mockkObject(TelegramApi)
        val msgResp = mockk<com.github.kotlintelegrambot.network.Response<com.github.kotlintelegrambot.entities.Message>>(relaxed = true)
        io.mockk.every { TelegramApi.sendMessage(any(), any(), any(), any(), any()) } returns msgResp
        WaitlistNotifierHolder.notifier = object : WaitlistNotifier {
            override fun onNewBooking() { TelegramApi.sendMessage(ChatId.fromId(1), "ok") }
            override fun onCancel() {}
        }
        val service = BookingService()
        val booking = service.createBooking(
            BookingRequest(
                userId = 1,
                clubId = 1,
                tableId = 1,
                bookingTime = Instant.EPOCH,
                partySize = 2,
                expectedDuration = 60,
                bookingGuestName = "A",
                telegramId = 1,
                phone = "+10000000000",
                promoterId = null,
                bookingSource = "test"
            )
        )
        assertTrue(booking.id > 0)
        verify { TelegramApi.sendMessage(any(), any(), any(), any(), any()) }
    }
}
