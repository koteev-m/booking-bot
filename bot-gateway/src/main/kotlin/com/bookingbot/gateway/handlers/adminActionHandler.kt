package com.bookingbot.gateway.handlers

import com.bookingbot.api.services.BookingService
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ParseMode

fun addAdminActionHandler(dispatcher: Dispatcher, bookingService: BookingService) {
    dispatcher.callbackQuery {
        val data = callbackQuery.data
        val message = callbackQuery.message ?: return@callbackQuery
        val chatId = message.chat.id
        val messageId = message.messageId

        when {
            data.startsWith("admin_confirm_") -> {
                val bookingId = data.removePrefix("admin_confirm_").toInt()
                bookingService.updateBookingStatus(bookingId, "SEATED")
                bot.editMessageText(
                    chatId = chatId,
                    messageId = messageId,
                    text = message.text + "\n\n✅ Гости на месте.",
                    parseMode = ParseMode.MARKDOWN,
                    replyMarkup = null // Убираем кнопки
                )
                bot.answerCallbackQuery(callbackQuery.id, text = "Статус обновлен: Гости пришли.")
            }
            data.startsWith("admin_noshow_") -> {
                val bookingId = data.removePrefix("admin_noshow_").toInt()
                bookingService.updateBookingStatus(bookingId, "NO_SHOW")
                bot.editMessageText(
                    chatId = chatId,
                    messageId = messageId,
                    text = message.text + "\n\n❌ Неявка.",
                    parseMode = ParseMode.MARKDOWN,
                    replyMarkup = null // Убираем кнопки
                )
                bot.answerCallbackQuery(callbackQuery.id, text = "Статус обновлен: Неявка.")
            }
        }
    }
}
