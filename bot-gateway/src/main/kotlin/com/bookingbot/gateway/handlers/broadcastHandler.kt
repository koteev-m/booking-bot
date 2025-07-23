package com.bookingbot.gateway.handlers

import com.bookingbot.api.model.UserRole
import com.bookingbot.api.services.UserService
import com.bookingbot.gateway.fsm.State
import com.bookingbot.gateway.fsm.StateStorage
import com.bookingbot.gateway.util.StateFilter
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun addBroadcastHandler(dispatcher: Dispatcher, userService: UserService) {

    // Шаг 1: Админ нажимает "Создать рассылку"
    dispatcher.callbackQuery("admin_create_broadcast") {
        val adminId = callbackQuery.from.id
        val admin = userService.findOrCreateUser(adminId, null)

        if (admin.role != UserRole.ADMIN && admin.role != UserRole.OWNER) {
            bot.answerCallbackQuery(callbackQuery.id, "У вас нет прав для этого действия.", showAlert = true)
            return@callbackQuery
        }

        StateStorage.setState(adminId, State.BroadcastMessageInput)
        bot.sendMessage(ChatId.fromId(adminId), "Введите сообщение для рассылки. Вы можете использовать форматирование и прикрепить фото.")
    }

    // Шаг 2: Админ отправляет сообщение для рассылки (любого типа)
    dispatcher.message(StateFilter(State.BroadcastMessageInput.key)) {
        val adminId = message.from?.id ?: return@message

        // Сохраняем ID сообщения, чтобы потом его переслать
        StateStorage.getContext(adminId).broadcastMessageId = message.messageId

        val userCount = userService.getAllUserIds().size
        val confirmationKeyboard = InlineKeyboardMarkup.create(listOf(
            InlineKeyboardButton.CallbackData("✅ Отправить всем ($userCount чел.)", "broadcast_confirm_send"),
            InlineKeyboardButton.CallbackData("❌ Отмена", "broadcast_cancel")
        ))

        StateStorage.setState(adminId, State.BroadcastConfirmation)
        bot.sendMessage(ChatId.fromId(adminId), "Вы уверены, что хотите отправить это сообщение всем пользователям?", replyMarkup = confirmationKeyboard)
    }

    // Шаг 3: Админ подтверждает или отменяет рассылку
    dispatcher.callbackQuery {
        if (StateStorage.getState(callbackQuery.from.id) != State.BroadcastConfirmation.key) return@callbackQuery

        val adminId = callbackQuery.from.id
        val context = StateStorage.getContext(adminId)
        val messageIdToForward = context.broadcastMessageId

        when (callbackQuery.data) {
            "broadcast_confirm_send" -> {
                if (messageIdToForward == null) {
                    bot.sendMessage(ChatId.fromId(adminId), "Ошибка: не найдено сообщение для рассылки.")
                    StateStorage.clear(adminId)
                    return@callbackQuery
                }

                bot.editMessageText(ChatId.fromId(adminId), callbackQuery.message!!.messageId, text = "Начинаю рассылку...")

                val userIds = userService.getAllUserIds()
                var successCount = 0
                var failCount = 0

                // Запускаем рассылку в отдельной корутине, чтобы не блокировать бота
                GlobalScope.launch {
                    userIds.forEach { userId ->
                        try {
                            // Используем forwardMessage, чтобы сохранить форматирование, фото и т.д.
                            bot.forwardMessage(
                                chatId = ChatId.fromId(userId),
                                fromChatId = ChatId.fromId(adminId),
                                messageId = messageIdToForward
                            )
                            successCount++
                            // Небольшая задержка, чтобы не превышать лимиты Telegram
                            delay(100)
                        } catch (e: Exception) {
                            failCount++
                        }
                    }
                    bot.sendMessage(ChatId.fromId(adminId), "✅ Рассылка завершена.\nУспешно: $successCount\nНе удалось: $failCount")
                }
            }
            "broadcast_cancel" -> {
                bot.editMessageText(ChatId.fromId(adminId), callbackQuery.message!!.messageId, text = "Рассылка отменена.")
            }
        }
        StateStorage.clear(adminId)
    }
}