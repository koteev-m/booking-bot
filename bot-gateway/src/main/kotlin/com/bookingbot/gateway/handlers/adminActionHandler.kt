package com.bookingbot.gateway.handlers

import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.ClubService
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode

fun addAdminActionHandler(dispatcher: Dispatcher, bookingService: BookingService, clubService: ClubService) {
    // Этот обработчик "слушает" нажатия на все inline-кнопки, которые видит бот
    dispatcher.callbackQuery {
        val data = callbackQuery.data
        val message = callbackQuery.message ?: return@callbackQuery
        val chatId = message.chat.id
        val messageId = message.messageId

        when {
            // Если callback_data начинается с "admin_confirm_"
            data.startsWith("admin_confirm_") -> {
                val bookingId = data.removePrefix("admin_confirm_").toInt()
                // Обновляем статус брони в базе данных на "SEATED" (гости пришли)
                bookingService.updateBookingStatus(bookingId, "SEATED")

                // Редактируем исходное сообщение в канале, добавляя отметку и убирая кнопки
                bot.editMessageText(
                    chatId = ChatId.fromId(chatId), // <<< ИСПРАВЛЕНО
                    messageId = messageId,
                    text = message.text + "\n\n✅ Гости на месте.",
                    parseMode = ParseMode.MARKDOWN,
                    replyMarkup = null // Убираем кнопки, чтобы избежать повторных нажатий
                )
                // Отправляем короткое всплывающее уведомление администратору, который нажал кнопку
                bot.answerCallbackQuery(callbackQuery.id, text = "Статус обновлен: Гости пришли.")
            }

            // Если callback_data начинается с "admin_noshow_"
            data.startsWith("admin_noshow_") -> {
                val bookingId = data.removePrefix("admin_noshow_").toInt()
                // Обновляем статус брони в базе данных на "NO_SHOW" (неявка)
                bookingService.updateBookingStatus(bookingId, "NO_SHOW")

                bot.editMessageText(
                    chatId = ChatId.fromId(chatId), // <<< ИСПРАВЛЕНО
                    messageId = messageId,
                    text = message.text + "\n\n❌ Неявка.",
                    parseMode = ParseMode.MARKDOWN,
                    replyMarkup = null
                )
                bot.answerCallbackQuery(callbackQuery.id, text = "Статус обновлен: Неявка.")
            }

            data.startsWith("admin_cancel_") -> {
                val bookingId = data.removePrefix("admin_cancel_").toInt()
                val bookingToCancel = bookingService.findBookingById(bookingId) // Находим бронь перед отменой

                if (bookingToCancel != null && bookingService.cancelBookingByStaff(bookingId)) {
                    bot.editMessageText(
                        chatId = ChatId.fromId(chatId),
                        messageId = messageId,
                        text = message.text + "\n\n🚫 Бронь отменена менеджером.",
                        parseMode = ParseMode.MARKDOWN,
                        replyMarkup = null
                    )
                    bot.answerCallbackQuery(callbackQuery.id, text = "Бронь №$bookingId отменена.")

                    // <<< НАЧАЛО: Отправляем уведомление гостю
                    val club = clubService.findClubById(bookingToCancel.clubId)
                    val guestNotificationText = """
                        К сожалению, ваша бронь №${bookingToCancel.id} в клубе *${club?.name ?: "N/A"}* была отменена администрацией.
                        
                        Для уточнения деталей, пожалуйста, свяжитесь с клубом.
                    """.trimIndent()

                    bot.sendMessage(
                        chatId = ChatId.fromId(bookingToCancel.userId),
                        text = guestNotificationText,
                        parseMode = ParseMode.MARKDOWN
                    )
                    // <<< КОНЕЦ: Отправляем уведомление гостю

                } else {
                    bot.answerCallbackQuery(callbackQuery.id, text = "Не удалось отменить бронь.", showAlert = true)
                }
            }

            // Если callback_data начинается с "admin_cancel_"
            data.startsWith("admin_cancel_") -> {
                val bookingId = data.removePrefix("admin_cancel_").toInt()
                // Отменяем бронь от имени персонала
                if (bookingService.cancelBookingByStaff(bookingId)) {
                    bot.editMessageText(
                        chatId = ChatId.fromId(chatId), // <<< ИСПРАВЛЕНО
                        messageId = messageId,
                        text = message.text + "\n\n🚫 Бронь отменена менеджером.",
                        parseMode = ParseMode.MARKDOWN,
                        replyMarkup = null
                    )
                    bot.answerCallbackQuery(callbackQuery.id, text = "Бронь №$bookingId отменена.")
                    // TODO: Отправить уведомление гостю об отмене его брони
                } else {
                    bot.answerCallbackQuery(callbackQuery.id, text = "Не удалось отменить бронь.", showAlert = true)
                }
            }
        }
    }
}
