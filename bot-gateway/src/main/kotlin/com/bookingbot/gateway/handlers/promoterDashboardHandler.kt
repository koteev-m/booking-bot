package com.bookingbot.gateway.handlers
import com.bookingbot.gateway.TelegramApi

import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.ClubService
import com.bookingbot.gateway.util.escapeMarkdownV2 // <<< ДОБАВЛЕН ИМПОРТ
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.bookingbot.gateway.util.CallbackData
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun addPromoterDashboardHandler(dispatcher: Dispatcher, bookingService: BookingService, clubService: ClubService) {

    // Обработчик для кнопки "Мои брони (промо)"
    dispatcher.callbackQuery(CallbackData.PROMOTER_MY_BOOKINGS) {
        val promoterId = callbackQuery.from.id
        val chatId = ChatId.fromId(promoterId)

        val bookings = bookingService.findBookingsByPromoterId(promoterId)

        if (bookings.isEmpty()) {
            TelegramApi.sendMessage(chatId, "Вы еще не создали ни одного бронирования для гостей.")
            return@callbackQuery
        }

        TelegramApi.sendMessage(chatId, "*Ваши бронирования для гостей:*", parseMode = ParseMode.MARKDOWN)

        val formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault())
        bookings.forEach { booking ->
            val club = clubService.findClubById(booking.clubId)
            val clubName = club?.name ?: "Неизвестный клуб"

            val bookingInfo = """
                *Гость:* ${booking.bookingGuestName ?: "N/A"}
                *Клуб:* ${clubName.escapeMarkdownV2()} | *ID брони:* ${booking.id}
                *Стол:* ${booking.tableId} | *Гостей:* ${booking.partySize}
                *Дата:* ${formatter.format(booking.bookingTime)}
                *Статус:* `${booking.status}`
            """.trimIndent()

            TelegramApi.sendMessage(
                chatId = chatId,
                text = bookingInfo,
                parseMode = ParseMode.MARKDOWN_V2
            )
        }
    }
}