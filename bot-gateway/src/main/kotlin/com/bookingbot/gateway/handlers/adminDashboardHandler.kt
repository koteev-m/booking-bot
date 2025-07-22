package com.bookingbot.gateway.handlers

import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.UserService
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun addAdminDashboardHandler(dispatcher: Dispatcher, userService: UserService, bookingService: BookingService) {

    // Обработчик для кнопки "Управление бронями"
    dispatcher.callbackQuery("admin_manage_bookings") {
        val adminId = callbackQuery.from.id
        val chatId = ChatId.fromId(adminId)

        // Находим клуб, к которому привязан админ
        val clubId = userService.getStaffClubId(adminId)
        if (clubId == null) {
            bot.answerCallbackQuery(callbackQuery.id, "Вы не привязаны ни к одному клубу.", showAlert = true)
            return@callbackQuery
        }

        val bookings = bookingService.findActiveBookingsByClub(clubId)

        if (bookings.isEmpty()) {
            bot.sendMessage(chatId, "В вашем клубе нет активных броней.")
            return@callbackQuery
        }

        bot.sendMessage(chatId, "*Активные брони в вашем клубе:*", parseMode = ParseMode.MARKDOWN)

        val formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault())
        bookings.forEach { booking ->
            val bookingInfo = """
                *ID ${booking.id}* | ${formatter.format(booking.bookingTime)}
                *Гость:* ${booking.bookingGuestName ?: "N/A"}
                *Стол:* ${booking.tableId} | *Гостей:* ${booking.partySize}
                *Источник:* ${booking.source} | *Статус:* `${booking.status}`
            """.trimIndent()

            // Кнопки для управления бронью (те же, что и в канале)
            val adminKeyboard = InlineKeyboardMarkup.create(
                listOf(
                    InlineKeyboardButton.CallbackData("✅ Пришли", "admin_confirm_${booking.id}"),
                    InlineKeyboardButton.CallbackData("❌ Неявка", "admin_noshow_${booking.id}"),
                    InlineKeyboardButton.CallbackData("🚫 Отменить", "admin_cancel_${booking.id}")
                )
            )

            bot.sendMessage(
                chatId = chatId,
                text = bookingInfo,
                parseMode = ParseMode.MARKDOWN,
                replyMarkup = adminKeyboard
            )
        }
    }
}