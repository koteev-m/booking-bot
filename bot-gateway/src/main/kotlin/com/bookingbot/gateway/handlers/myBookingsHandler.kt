package com.bookingbot.gateway.handlers

import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.ClubService
import com.bookingbot.gateway.util.escapeMarkdownV2
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun addMyBookingsHandler(dispatcher: Dispatcher, bookingService: BookingService, clubService: ClubService) {
    // Обработчик для кнопки "Мои бронирования"
    dispatcher.callbackQuery("my_bookings") {
        val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)
        val bookings = bookingService.findBookingsByUserId(chatId.id)

        if (bookings.isEmpty()) {
            bot.sendMessage(chatId, "У вас пока нет активных бронирований.")
            return@callbackQuery
        }

        val formatter = DateTimeFormatter.ofPattern("dd\\.MM\\.yyyy HH:mm").withZone(ZoneId.systemDefault())

        bookings.forEach { booking ->
            val club = clubService.findClubById(booking.clubId)
            val clubName = club?.name ?: "Неизвестный клуб"

            val bookingInfo = """
                *Бронь №${booking.id}*
                *Клуб:* ${clubName.escapeMarkdownV2()}
                *Стол ID:* ${booking.tableId}
                *Дата:* ${formatter.format(booking.bookingTime)}
                *Гостей:* ${booking.partySize}
                *Статус:* `${booking.status}`
            """.trimIndent()

            val bookingMenu = if (booking.status != "CANCELLED") {
                InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData("Изменить", "edit_booking_${booking.id}"),
                        InlineKeyboardButton.CallbackData("Отменить", "cancel_booking_${booking.id}")
                    )
                )
            } else {
                null
            }

            bot.sendMessage(
                chatId = chatId,
                text = bookingInfo,
                replyMarkup = bookingMenu,
                parseMode = ParseMode.MARKDOWN_V2
            )
        }
    }

    // Обработчик для кнопок "Изменить" и "Отменить"
    dispatcher.callbackQuery {
        val data = callbackQuery.data
        val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)
        val userId = callbackQuery.from.id

        when {
            data.startsWith("edit_booking_") -> {
                bot.answerCallbackQuery(callbackQuery.id, text = "Функция изменения пока не реализована.", showAlert = true)
            }
            data.startsWith("cancel_booking_") -> {
                val bookingId = data.removePrefix("cancel_booking_").toIntOrNull()
                if (bookingId == null) {
                    bot.answerCallbackQuery(callbackQuery.id, "Ошибка: неверный ID брони.", showAlert = true)
                    return@callbackQuery
                }

                val success = bookingService.cancelBooking(bookingId, userId)

                if (success) {
                    bot.answerCallbackQuery(callbackQuery.id, text = "Бронь №$bookingId отменена.")
                    val updatedBooking = bookingService.findBookingById(bookingId)
                    if (updatedBooking != null) {
                        val club = clubService.findClubById(updatedBooking.clubId)
                        val clubName = club?.name ?: "Неизвестный клуб"
                        val formatter = DateTimeFormatter.ofPattern("dd\\.MM\\.yyyy HH:mm").withZone(ZoneId.systemDefault())
                        val updatedText = """
                            *Бронь №${updatedBooking.id}*
                            *Клуб:* ${clubName.escapeMarkdownV2()}
                            *Стол ID:* ${updatedBooking.tableId}
                            *Дата:* ${formatter.format(updatedBooking.bookingTime)}
                            *Гостей:* ${updatedBooking.partySize}
                            *Статус:* `${updatedBooking.status}`
                        """.trimIndent()

                        bot.editMessageText(
                            chatId = chatId,
                            messageId = callbackQuery.message!!.messageId,
                            text = updatedText,
                            parseMode = ParseMode.MARKDOWN_V2,
                            replyMarkup = null // Убираем кнопки
                        )
                    }
                } else {
                    bot.answerCallbackQuery(callbackQuery.id, text = "Не удалось отменить бронь. Возможно, она вам не принадлежит.", showAlert = true)
                }
            }
        }
    }
}
