package com.bookingbot.gateway.handlers
import com.bookingbot.gateway.TelegramApi

import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.UserService
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.bookingbot.gateway.util.CallbackData
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun addAdminDashboardHandler(dispatcher: Dispatcher, userService: UserService, bookingService: BookingService) {

    // Обработчик для кнопки "Управление бронями"
    dispatcher.callbackQuery(CallbackData.ADMIN_MANAGE_BOOKINGS) {
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
            TelegramApi.sendMessage(chatId, "В вашем клубе нет активных броней.")
            return@callbackQuery
        }

        TelegramApi.sendMessage(chatId, "*Активные брони в вашем клубе:*", parseMode = ParseMode.MARKDOWN)

        val formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault())
        bookings.forEach { booking ->
            val bookingInfo = """
                *ID ${booking.id}* | ${formatter.format(booking.bookingTime)}
                *Гость:* ${booking.bookingGuestName ?: "N/A"}
                *Стол:* ${booking.tableId} | *Гостей:* ${booking.partySize}
                *Источник:* ${booking.bookingSource} | *Статус:* `${booking.status}`
            """.trimIndent()

            // Кнопки для управления бронью (те же, что и в канале)
            val adminKeyboard = InlineKeyboardMarkup.create(
                listOf(
                    InlineKeyboardButton.CallbackData("✅ Пришли", "${CallbackData.ADMIN_CONFIRM_PREFIX}${booking.id}"),
                    InlineKeyboardButton.CallbackData("❌ Неявка", "${CallbackData.ADMIN_NOSHOW_PREFIX}${booking.id}"),
                    InlineKeyboardButton.CallbackData("🚫 Отменить", "${CallbackData.ADMIN_CANCEL_PREFIX}${booking.id}")
                )
            )

            TelegramApi.sendMessage(
                chatId = chatId,
                text = bookingInfo,
                parseMode = ParseMode.MARKDOWN,
                replyMarkup = adminKeyboard
            )
        }
    }
}