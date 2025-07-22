package com.bookingbot.gateway.handlers

import com.bookingbot.api.model.UserRole
import com.bookingbot.api.services.ClubService
import com.bookingbot.api.services.UserService
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode

fun addAdminHandlers(dispatcher: Dispatcher, userService: UserService, clubService: ClubService) {

    // Команда для ответа пользователю
    dispatcher.command("answer") {
        val adminId = message.from?.id ?: return@command
        val admin = userService.findOrCreateUser(adminId, null)

        // Проверяем, что команду использует админ или владелец
        if (admin.role != UserRole.ADMIN && admin.role != UserRole.OWNER) {
            bot.sendMessage(ChatId.fromId(adminId), "У вас нет прав для использования этой команды.")
            return@command
        }

        // Парсим аргументы: /answer <userId> <текст ответа>
        val commandParts = message.text?.split(" ") ?: return@command
        if (commandParts.size < 3) {
            bot.sendMessage(
                ChatId.fromId(adminId),
                "Неверный формат. Используйте: `/answer <ID пользователя> <текст ответа>`",
                parseMode = ParseMode.MARKDOWN
            )
            return@command
        }

        val targetUserId = commandParts[1].toLongOrNull()
        if (targetUserId == null) {
            bot.sendMessage(ChatId.fromId(adminId), "Неверный ID пользователя.")
            return@command
        }

        val answerText = commandParts.drop(2).joinToString(" ")

        // Отправляем ответ пользователю
        val result = bot.sendMessage(
            chatId = ChatId.fromId(targetUserId),
            text = "💬 *Ответ от администрации:*\n\n$answerText",
            parseMode = ParseMode.MARKDOWN
        )

        if (result.isSuccess) {
            bot.sendMessage(ChatId.fromId(adminId), "✅ Ответ успешно отправлен пользователю $targetUserId.")
        } else {
            bot.sendMessage(ChatId.fromId(adminId), "❌ Не удалось отправить ответ. Возможно, пользователь заблокировал бота.")
        }
    }
}