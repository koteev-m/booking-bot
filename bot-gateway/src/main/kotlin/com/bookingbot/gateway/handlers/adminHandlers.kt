package com.bookingbot.gateway.handlers
import com.bookingbot.gateway.TelegramApi

import com.bookingbot.api.model.UserRole
import com.bookingbot.api.services.ClubService
import com.bookingbot.api.services.UserService
import com.bookingbot.gateway.Bot
import com.bookingbot.gateway.fsm.State
import com.bookingbot.gateway.fsm.StateStorage
import com.bookingbot.gateway.util.StateFilter
import com.bookingbot.gateway.util.CallbackData
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter

fun addAdminHandlers(dispatcher: Dispatcher, userService: UserService, clubService: ClubService) {

    // Команда для ответа пользователю на вопрос
    dispatcher.command("answer") {
        val adminId = message.from?.id ?: return@command
        val admin = userService.findOrCreateUser(adminId, null)

        // Проверяем, что команду использует админ или владелец
        if (admin.role != UserRole.ADMIN && admin.role != UserRole.OWNER) {
            TelegramApi.sendMessage(ChatId.fromId(adminId), "У вас нет прав для использования этой команды.")
            return@command
        }

        val commandParts = message.text?.split(" ") ?: return@command
        if (commandParts.size < 3) {
            TelegramApi.sendMessage(
                ChatId.fromId(adminId),
                "Неверный формат. Используйте: `/answer <ID пользователя> <текст ответа>`",
                parseMode = ParseMode.MARKDOWN
            )
            return@command
        }

        val targetUserId = commandParts[1].toLongOrNull()
        if (targetUserId == null) {
            TelegramApi.sendMessage(ChatId.fromId(adminId), "Неверный ID пользователя.")
            return@command
        }

        val answerText = commandParts.drop(2).joinToString(" ")

        val result = TelegramApi.sendMessage(
            chatId = ChatId.fromId(targetUserId),
            text = "💬 *Ответ от администрации:*\n\n$answerText",
            parseMode = ParseMode.MARKDOWN
        )

        if (result.isSuccess) {
            TelegramApi.sendMessage(ChatId.fromId(adminId), "✅ Ответ успешно отправлен пользователю $targetUserId.")
        } else {
            TelegramApi.sendMessage(ChatId.fromId(adminId), "❌ Не удалось отправить ответ. Возможно, пользователь заблокировал бота.")
        }
    }

    // Команда для назначения ролей (только для владельцев)
    dispatcher.command("setrole") {
        val requesterId = message.from?.id ?: return@command

        if (requesterId !in Bot.OWNER_IDS) {
            TelegramApi.sendMessage(ChatId.fromId(requesterId), "Эта команда доступна только владельцам бота.")
            return@command
        }

        val commandParts = message.text?.split(" ") ?: return@command
        if (commandParts.size != 3) {
            TelegramApi.sendMessage(
                ChatId.fromId(requesterId),
                "Неверный формат. Используйте: `/setrole <ID пользователя> <ROLE>`\nДоступные роли: GUEST, PROMOTER, ADMIN, OWNER",
                parseMode = ParseMode.MARKDOWN
            )
            return@command
        }

        val targetUserId = commandParts[1].toLongOrNull()
        val roleName = commandParts[2].uppercase()

        if (targetUserId == null) {
            TelegramApi.sendMessage(ChatId.fromId(requesterId), "Неверный ID пользователя.")
            return@command
        }

        val newRole = try {
            UserRole.valueOf(roleName)
        } catch (e: IllegalArgumentException) {
            TelegramApi.sendMessage(ChatId.fromId(requesterId), "Неверная роль. Доступные: GUEST, PROMOTER, ADMIN, OWNER.")
            return@command
        }

        if (userService.updateUserRole(targetUserId, newRole)) {
            TelegramApi.sendMessage(ChatId.fromId(requesterId), "✅ Роль для пользователя $targetUserId успешно изменена на $newRole.")
            TelegramApi.sendMessage(ChatId.fromId(targetUserId), "Вам была назначена новая роль: *$newRole*", parseMode = ParseMode.MARKDOWN)
        } else {
            TelegramApi.sendMessage(ChatId.fromId(requesterId), "❌ Не удалось найти пользователя с ID $targetUserId.")
        }
    }

    // Команда для привязки персонала к клубу (только для владельцев)
    dispatcher.command("addstaff") {
        val requesterId = message.from?.id ?: return@command

        if (requesterId !in Bot.OWNER_IDS) {
            TelegramApi.sendMessage(ChatId.fromId(requesterId), "Эта команда доступна только владельцам бота.")
            return@command
        }

        val commandParts = message.text?.split(" ") ?: return@command
        if (commandParts.size != 4) {
            TelegramApi.sendMessage(
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
            TelegramApi.sendMessage(ChatId.fromId(requesterId), "Неверный формат ID пользователя или клуба.")
            return@command
        }

        val role = try { UserRole.valueOf(roleName) } catch (e: Exception) { null }

        if (role != UserRole.ADMIN && role != UserRole.PROMOTER) {
            TelegramApi.sendMessage(ChatId.fromId(requesterId), "Неверная роль. Доступные: ADMIN, PROMOTER.")
            return@command
        }

        if (clubService.findClubById(targetClubId) == null) {
            TelegramApi.sendMessage(ChatId.fromId(requesterId), "Клуб с ID $targetClubId не найден.")
            return@command
        }

        userService.findOrCreateUser(targetUserId, null)

        if (userService.assignUserToClub(targetUserId, targetClubId, role)) {
            TelegramApi.sendMessage(ChatId.fromId(requesterId), "✅ Пользователь $targetUserId успешно назначен на роль $role в клубе $targetClubId.")
            TelegramApi.sendMessage(ChatId.fromId(targetUserId), "Вам назначена роль *$role* для клуба (ID: $targetClubId).", parseMode = ParseMode.MARKDOWN)
        } else {
            TelegramApi.sendMessage(ChatId.fromId(requesterId), "❌ Не удалось назначить пользователя.")
        }
    }

    // Команда для создания брони вручную
    dispatcher.command("createbooking") {
        val adminId = message.from?.id ?: return@command
        val admin = userService.findOrCreateUser(adminId, null)

        if (admin.role != UserRole.ADMIN && admin.role != UserRole.OWNER) {
            TelegramApi.sendMessage(ChatId.fromId(adminId), "У вас нет прав для использования этой команды.")
            return@command
        }

        StateStorage.setState(adminId, State.AdminBookingGuestName)
        TelegramApi.sendMessage(ChatId.fromId(adminId), "Начинаем создание брони. Введите имя гостя:")
    }

    // Админ вводит имя гостя
    dispatcher.message(Filter.Text and StateFilter(State.AdminBookingGuestName.key)) {
        val adminId = message.from?.id ?: return@message
        val guestName = message.text ?: return@message

        StateStorage.getContext(adminId).bookingGuestName = guestName
        StateStorage.setState(adminId, State.AdminBookingSource)
        TelegramApi.sendMessage(ChatId.fromId(adminId), "Имя гостя '$guestName' принято. Теперь введите источник брони (например, 'Звонок', 'Instagram'):")
    }

    // Админ вводит источник брони
    dispatcher.message(Filter.Text and StateFilter(State.AdminBookingSource.key)) {
        val adminId = message.from?.id ?: return@message
        val source = message.text ?: return@message

        StateStorage.getContext(adminId).source = source
        StateStorage.setState(adminId, State.AdminBookingPhone)
        TelegramApi.sendMessage(ChatId.fromId(adminId), "Источник '$source' принят. Теперь введите номер телефона гостя:")
    }

    // Админ вводит номер телефона гостя и переходит к выбору клуба
    dispatcher.message(Filter.Text and StateFilter(State.AdminBookingPhone.key)) {
        val adminId = message.from?.id ?: return@message
        val phone = message.text

        val phoneRegex = """^\+?\d{10,14}$""".toRegex()
        if (phone == null || !phone.matches(phoneRegex)) {
            TelegramApi.sendMessage(ChatId.fromId(adminId), "Неверный формат номера. Пожалуйста, введите номер в международном формате, например: +79991234567")
            return@message
        }

        StateStorage.getContext(adminId).phone = phone

        // <<< НАЧАЛО: Перенаправляем на стандартный флоу выбора клуба
        val clubs = clubService.getAllClubs()
        val clubButtons = clubs.map {
            InlineKeyboardButton.CallbackData(it.name, "${CallbackData.SHOW_CLUB_PREFIX}${it.id}")
        }.chunked(2)

        TelegramApi.sendMessage(
            chatId = ChatId.fromId(adminId),
            text = "Телефон '$phone' принят. Теперь выберите клуб для бронирования:",
            replyMarkup = InlineKeyboardMarkup.create(clubButtons)
        )
        // Переводим админа в состояние выбора клуба, чтобы FSM гостя подхватил диалог
        StateStorage.setState(adminId, State.ClubSelection)
        // <<< КОНЕЦ: Перенаправляем на стандартный флоу выбора клуба
    }
}
