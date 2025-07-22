package com.bookingbot.gateway.handlers

import com.bookingbot.api.model.UserRole
import com.bookingbot.api.services.UserService
import com.bookingbot.gateway.markup.Menus
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId

fun addStartHandler(dispatcher: Dispatcher, userService: UserService) {
    dispatcher.command("start") {
        val user = userService.findOrCreateUser(message.chat.id, message.chat.username)

        val (menuText, menuMarkup) = when (user.role) {
            UserRole.GUEST -> "Выберите действие:" to Menus.guestMenu()
            UserRole.PROMOTER -> "Панель промоутера. Выберите действие:" to Menus.promoterMenu()
            UserRole.ADMIN -> "Панель администратора. Выберите действие:" to Menus.adminMenu()
            UserRole.OWNER -> "Панель владельца. Выберите действие:" to Menus.adminMenu() // Владелец пока использует меню админа
        }

        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "Здравствуйте, ${user.username ?: "Гость"}! 👋\nВаша роль: ${user.role}.\n\n$menuText",
            replyMarkup = menuMarkup
        )
    }
}
