package com.bookingbot.gateway.handlers

import com.bookingbot.api.services.BookingService
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun addMyBookingsHandler(dispatcher: Dispatcher, bookingService: BookingService) {
    dispatcher.callbackQuery("my_bookings") {
        val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)
        val bookings = bookingService.findBookingsByUserId(chatId.id)

        if (bookings.isEmpty()) {
            bot.sendMessage(chatId, "У вас пока нет активных бронирований.")
            return@callbackQuery
        }

        val formatter = DateTimeFormatter.ofPattern("dd\\.MM\\.yyyy HH:mm").withZone(ZoneId.systemDefault())

        bookings.forEach { booking ->
            val bookingInfo = """
                *Бронь №${booking.id}*
                Клуб ID: ${booking.clubId}
                Стол ID: ${booking.tableId}
                Дата: ${formatter.format(booking.bookingTime)}
                Гостей: ${booking.partySize}
                Статус: `${booking.status}`
            """.trimIndent()

            // Не показываем кнопки для уже отмененных броней
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
                    // Обновляем исходное сообщение, чтобы убрать кнопки и показать новый статус
                    val updatedBooking = bookingService.findBookingById(bookingId)
                    if (updatedBooking != null) {
                        val formatter = DateTimeFormatter.ofPattern("dd\\.MM\\.yyyy HH:mm").withZone(ZoneId.systemDefault())
                        val updatedText = """
                            *Бронь №${updatedBooking.id}*
                            Клуб ID: ${updatedBooking.clubId}
                            Стол ID: ${updatedBooking.tableId}
                            Дата: ${formatter.format(updatedBooking.bookingTime)}
                            Гостей: ${updatedBooking.partySize}
                            Статус: `${updatedBooking.status}`
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
                    bot.answerCallbackQuery(callbackQuery.id, text = "Не удалось отменить бронь.", showAlert = true)
                }
            }
        }
    }
}

