package com.bookingbot.gateway.markup

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

object Menus {

    val backToMainMenuButton = InlineKeyboardButton.CallbackData("⬅️ Главное меню", "back_to_main_menu")

    fun guestMenu(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup.create(
            listOf(
                listOf(InlineKeyboardButton.CallbackData(text = "Выбрать клуб", callbackData = "select_club")),
                listOf(InlineKeyboardButton.CallbackData(text = "Мои бронирования", callbackData = "my_bookings")),
                listOf(InlineKeyboardButton.CallbackData(text = "Задать вопрос", callbackData = "ask_question")),
                listOf(InlineKeyboardButton.CallbackData(text = "Музыка", callbackData = "music_sets"))
            )
        )
    }

    fun clubMenu(clubId: Int): InlineKeyboardMarkup {
        return InlineKeyboardMarkup.create(
            listOf(
                listOf(InlineKeyboardButton.CallbackData(text = "Забронировать стол", callbackData = "start_booking_$clubId")),
                listOf(InlineKeyboardButton.CallbackData(text = "Информация", callbackData = "club_info_$clubId")),
                listOf(InlineKeyboardButton.CallbackData(text = "Фотоотчеты", callbackData = "club_photos_$clubId")),
                listOf(InlineKeyboardButton.CallbackData(text = "Афиши и события", callbackData = "club_events_$clubId")),
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
                listOf(InlineKeyboardButton.CallbackData(text = "Управление бронями", callbackData = "admin_manage_bookings")),
                listOf(InlineKeyboardButton.CallbackData(text = "Управление столами", callbackData = "admin_manage_tables")),
                listOf(InlineKeyboardButton.CallbackData(text = "Создать рассылку", callbackData = "admin_create_broadcast"))
            )
        )
    }
}