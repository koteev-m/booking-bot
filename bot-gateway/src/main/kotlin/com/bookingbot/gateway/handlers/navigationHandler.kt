package com.bookingbot.gateway.handlers
import com.bookingbot.gateway.TelegramApi

import com.bookingbot.api.model.UserRole
import com.bookingbot.api.services.UserService
import com.bookingbot.gateway.fsm.StateStorage
import com.bookingbot.gateway.markup.Menus
import com.bookingbot.gateway.util.CallbackData
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId

fun addNavigationHandler(dispatcher: Dispatcher, userService: UserService) {

    // Обработчик для команды /cancel
    dispatcher.command("cancel") {
        val userId = message.from?.id ?: return@command
        StateStorage.clear(userId)
        TelegramApi.sendMessage(ChatId.fromId(userId), "Действие отменено. Вы в главном меню.")
        // Повторно вызываем /start, чтобы показать актуальное меню
        dispatcher.command("start")?.handler?.handleUpdate(bot, update)
    }

    // Обработчик для inline-кнопки "Главное меню"
    dispatcher.callbackQuery(CallbackData.BACK_TO_MAIN_MENU) {
        val userId = callbackQuery.from.id
        val message = callbackQuery.message ?: return@callbackQuery
        StateStorage.clear(userId)

        // Удаляем предыдущее сообщение (например, меню клуба), чтобы не засорять чат
        bot.deleteMessage(ChatId.fromId(userId), message.messageId)

        // Повторно вызываем /start, чтобы показать актуальное меню
        // Создаем фейковый update, чтобы передать его в обработчик
        val fakeUpdate = update.copy(message = message.copy(text = "/start"))
        dispatcher.command("start")?.handler?.handleUpdate(bot, fakeUpdate)
    }
}
