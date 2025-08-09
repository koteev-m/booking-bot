package com.bookingbot.gateway.handlers

import com.bookingbot.gateway.TelegramApi
import com.bookingbot.gateway.markup.Menus
import com.bookingbot.gateway.util.CallbackData
import com.bookingbot.api.services.GuestListService
import com.bookingbot.api.services.UserService
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

fun addEntranceManagerHandlers(dispatcher: Dispatcher, userService: UserService, guestListService: GuestListService) {

    fun sendGuestList(managerId: Long, clubId: Int) {
        val guests = guestListService.getGuestsForClub(clubId)
        if (guests.isEmpty()) {
            TelegramApi.sendMessage(ChatId.fromId(managerId), "Список гостей пуст.")
            return
        }
        val rows = guests.map { guest ->
            val status = if (guest.checkedIn) "✅" else "❌"
            listOf(
                InlineKeyboardButton.CallbackData(
                    text = "$status ${guest.firstName} ${guest.lastName}",
                    callbackData = "${CallbackData.ENTRANCE_CHECKIN_PREFIX}${guest.id}"
                )
            )
        } + listOf(listOf(Menus.backToMainMenuButton))
        TelegramApi.sendMessage(
            chatId = ChatId.fromId(managerId),
            text = "Список гостей:",
            replyMarkup = InlineKeyboardMarkup.create(rows)
        )
    }

    dispatcher.callbackQuery(CallbackData.ENTRANCE_GUESTS) {
        val managerId = callbackQuery.from.id
        val clubId = userService.getStaffClubId(managerId)
        if (clubId == null) {
            bot.answerCallbackQuery(callbackQuery.id, text = "Вы не привязаны к клубу.", showAlert = true)
            return@callbackQuery
        }
        sendGuestList(managerId, clubId)
    }

    dispatcher.callbackQuery {
        val data = callbackQuery.data ?: return@callbackQuery
        if (data.startsWith(CallbackData.ENTRANCE_CHECKIN_PREFIX)) {
            val guestId = data.removePrefix(CallbackData.ENTRANCE_CHECKIN_PREFIX).toInt()
            val managerId = callbackQuery.from.id
            val clubId = userService.getStaffClubId(managerId)
            if (clubId == null) {
                bot.answerCallbackQuery(callbackQuery.id, text = "Вы не привязаны к клубу.", showAlert = true)
                return@callbackQuery
            }
            val success = guestListService.checkInGuest(guestId)
            val text = if (success) "Гость отмечен как прошедший." else "Гость не найден."
            bot.answerCallbackQuery(callbackQuery.id, text = text, showAlert = true)
            sendGuestList(managerId, clubId)
        }
    }
}

