package com.bookingbot.gateway.markup

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.bookingbot.gateway.util.CallbackData

object Menus {

    val backToMainMenuButton = InlineKeyboardButton.CallbackData("⬅️ Главное меню", CallbackData.BACK_TO_MAIN_MENU)

    fun guestMenu(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup.create(
            listOf(
                listOf(InlineKeyboardButton.CallbackData(text = "Выбрать клуб", callbackData = CallbackData.SELECT_CLUB)),
                listOf(InlineKeyboardButton.CallbackData(text = "Мои бронирования", callbackData = CallbackData.MY_BOOKINGS)),
                listOf(InlineKeyboardButton.CallbackData(text = "Задать вопрос", callbackData = CallbackData.ASK_QUESTION)),
                listOf(InlineKeyboardButton.CallbackData(text = "Музыка", callbackData = CallbackData.MUSIC_SETS))
            )
        )
    }

    fun clubMenu(clubId: Int): InlineKeyboardMarkup {
        return InlineKeyboardMarkup.create(
            listOf(
                listOf(InlineKeyboardButton.CallbackData(text = "Забронировать стол", callbackData = "${CallbackData.START_BOOKING_PREFIX}$clubId")),
                listOf(InlineKeyboardButton.CallbackData(text = "Информация", callbackData = "${CallbackData.CLUB_INFO_PREFIX}$clubId")),
                listOf(InlineKeyboardButton.CallbackData(text = "Фотоотчеты", callbackData = "${CallbackData.CLUB_PHOTOS_PREFIX}$clubId")),
                listOf(InlineKeyboardButton.CallbackData(text = "Афиши и события", callbackData = "${CallbackData.CLUB_EVENTS_PREFIX}$clubId")),
                listOf(backToMainMenuButton)
            )
        )
    }

    fun promoterMenu(): InlineKeyboardMarkup {
        // ...
        return guestMenu() // Заглушка
    }

    fun adminMenu(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup.create(
            listOf(
                listOf(InlineKeyboardButton.CallbackData(text = "Управление бронями", callbackData = CallbackData.ADMIN_MANAGE_BOOKINGS)),
                listOf(InlineKeyboardButton.CallbackData(text = "Управление столами", callbackData = CallbackData.ADMIN_MANAGE_TABLES)),
                listOf(InlineKeyboardButton.CallbackData(text = "Создать рассылку", callbackData = CallbackData.ADMIN_CREATE_BROADCAST))
            )
        )
    }
}