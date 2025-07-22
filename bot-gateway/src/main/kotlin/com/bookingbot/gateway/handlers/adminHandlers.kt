package com.bookingbot.gateway.handlers

import com.bookingbot.api.model.UserRole
import com.bookingbot.api.services.ClubService
import com.bookingbot.api.services.UserService
import com.bookingbot.gateway.Bot.OWNER_IDS // Импортируем список ID владельцев
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

    // Команда для назначения ролей (только для владельцев)
    dispatcher.command("setrole") {
        val requesterId = message.from?.id ?: return@command

        // Проверяем, что команду использует один из владельцев
        if (requesterId !in OWNER_IDS) {
            bot.sendMessage(ChatId.fromId(requesterId), "Эта команда доступна только владельцам бота.")
            return@command
        }

        val commandParts = message.text?.split(" ") ?: return@command
        if (commandParts.size != 3) {
            bot.sendMessage(
                ChatId.fromId(requesterId),
                "Неверный формат. Используйте: `/setrole <ID пользователя> <ROLE>`\nДоступные роли: GUEST, PROMOTER, ADMIN",
                parseMode = ParseMode.MARKDOWN
            )
            return@command
        }

        val targetUserId = commandParts[1].toLongOrNull()
        val roleName = commandParts[2].uppercase()

        if (targetUserId == null) {
            bot.sendMessage(ChatId.fromId(requesterId), "Неверный ID пользователя.")
            return@command
        }

        val newRole = try {
            UserRole.valueOf(roleName)
        } catch (e: IllegalArgumentException) {
            bot.sendMessage(ChatId.fromId(requesterId), "Неверная роль. Доступные: GUEST, PROMOTER, ADMIN.")
            return@command
        }

        if (userService.updateUserRole(targetUserId, newRole)) {
            bot.sendMessage(ChatId.fromId(requesterId), "✅ Роль для пользователя $targetUserId успешно изменена на $newRole.")
            // Уведомляем пользователя об изменении его роли
            bot.sendMessage(ChatId.fromId(targetUserId), "Вам была назначена новая роль: *$newRole*", parseMode = ParseMode.MARKDOWN)
        } else {
            bot.sendMessage(ChatId.fromId(requesterId), "❌ Не удалось найти пользователя с ID $targetUserId.")
        }
    }

    // Команда для привязки персонала к клубу (только для владельцев)
    dispatcher.command("addstaff") {
        val requesterId = message.from?.id ?: return@command

        if (requesterId !in OWNER_IDS) {
            bot.sendMessage(ChatId.fromId(requesterId), "Эта команда доступна только владельцам бота.")
            return@command
        }

        val commandParts = message.text?.split(" ") ?: return@command
        if (commandParts.size != 4) {
            bot.sendMessage(
                ChatId.fromId(requesterId),
                "Неверный формат. Используйте: `/addstaff <ID пользователя> <ID клуба> <ROLE>`\nРоли: ADMIN, PROMOTER",
                parseMode = ParseMode.MARKDOWN
            )
            return@command
        }

        val targetUserId = commandParts[1].toLongOrNull()
        val targetClubId = commandParts[2].toIntOrNull()
        val roleName = commandParts[3].uppercase()

        if (targetUserId == null || targetClubId == null) {
            bot.sendMessage(ChatId.fromId(requesterId), "Неверный формат ID пользователя или клуба.")
            return@command
        }

        val role = try {
            UserRole.valueOf(roleName)
        } catch (e: Exception) { null }

        if (role != UserRole.ADMIN && role != UserRole.PROMOTER) {
            bot.sendMessage(ChatId.fromId(requesterId), "Неверная роль. Доступные: ADMIN, PROMOTER.")
            return@command
        }

        // Проверяем, существует ли клуб
        if (clubService.findClubById(targetClubId) == null) {
            bot.sendMessage(ChatId.fromId(requesterId), "Клуб с ID $targetClubId не найден.")
            return@command
        }

        // Убеждаемся, что пользователь существует в нашей БД
        userService.findOrCreateUser(targetUserId, null)

        if (userService.assignUserToClub(targetUserId, targetClubId, role)) {
            bot.sendMessage(ChatId.fromId(requesterId), "✅ Пользователь $targetUserId успешно назначен на роль $role в клубе $targetClubId.")
            bot.sendMessage(ChatId.fromId(targetUserId), "Вам назначена роль *$role* для клуба (ID: $targetClubId).", parseMode = ParseMode.MARKDOWN)
        } else {
            bot.sendMessage(ChatId.fromId(requesterId), "❌ Не удалось назначить пользователя.")
        }
    }
}
