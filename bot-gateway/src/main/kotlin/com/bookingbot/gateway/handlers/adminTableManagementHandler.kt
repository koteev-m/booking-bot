package com.bookingbot.gateway.handlers
import com.bookingbot.gateway.TelegramApi

import com.bookingbot.api.services.TableService
import com.bookingbot.api.services.UserService
import com.bookingbot.gateway.fsm.State
import com.bookingbot.gateway.fsm.StateStorage
import com.bookingbot.gateway.util.StateFilter
import com.bookingbot.gateway.util.CallbackData
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter

fun addAdminTableManagementHandler(dispatcher: Dispatcher, tableService: TableService, userService: UserService) {
    // Шаг 1: Админ нажимает "Управление столами"
    dispatcher.callbackQuery(CallbackData.ADMIN_MANAGE_TABLES) {
        val adminId = callbackQuery.from.id

        // <<< НАЧАЛО: Проверяем, к какому клубу привязан админ
        val clubId = userService.getStaffClubId(adminId)
        if (clubId == null) {
            bot.answerCallbackQuery(callbackQuery.id, "Вы не привязаны ни к одному клубу. Обратитесь к владельцу.", showAlert = true)
            return@callbackQuery
        }
        // <<< КОНЕЦ: Проверяем, к какому клубу привязан админ

        val tables = tableService.getTablesForClub(clubId)
        if (tables.isEmpty()) {
            TelegramApi.sendMessage(ChatId.fromId(adminId), "В вашем клубе еще не добавлены столы.")
            return@callbackQuery
        }

        val tableButtons = tables.map {
            InlineKeyboardButton.CallbackData("Стол №${it.number} (вмест: ${it.capacity}, деп: ${it.minDeposit})", "${CallbackData.ADMIN_EDIT_TABLE_PREFIX}${it.id}")
        }.chunked(1)

        StateStorage.setState(adminId, State.AdminSelectTableToEdit)
        TelegramApi.sendMessage(ChatId.fromId(adminId), "Выберите стол для редактирования:", replyMarkup = InlineKeyboardMarkup.create(tableButtons))
    }

    // Шаг 2: Админ выбрал стол, спрашиваем, что редактировать
    dispatcher.callbackQuery {
        if (!callbackQuery.data.startsWith(CallbackData.ADMIN_EDIT_TABLE_PREFIX)) return@callbackQuery
        val adminId = callbackQuery.from.id
        if (StateStorage.getState(adminId) != State.AdminSelectTableToEdit.key) return@callbackQuery

        val tableId = callbackQuery.data.removePrefix(CallbackData.ADMIN_EDIT_TABLE_PREFIX).toInt()
        StateStorage.getContext(adminId).editingTableId = tableId

        val editOptions = InlineKeyboardMarkup.create(listOf(
            InlineKeyboardButton.CallbackData("Вместимость", CallbackData.EDIT_CAPACITY),
            InlineKeyboardButton.CallbackData("Депозит", CallbackData.EDIT_DEPOSIT)
        ))
        bot.editMessageText(ChatId.fromId(adminId), callbackQuery.message!!.messageId, text = "Что вы хотите изменить для стола №$tableId?", replyMarkup = editOptions)
    }

    // Шаг 3: Админ выбрал "Вместимость" или "Депозит"
    dispatcher.callbackQuery {
        val adminId = callbackQuery.from.id
        when(callbackQuery.data) {
            CallbackData.EDIT_CAPACITY -> {
                StateStorage.setState(adminId, State.AdminEditingTableCapacity)
                TelegramApi.sendMessage(ChatId.fromId(adminId), "Введите новую вместимость (число):")
            }
            CallbackData.EDIT_DEPOSIT -> {
                StateStorage.setState(adminId, State.AdminEditingTableDeposit)
                TelegramApi.sendMessage(ChatId.fromId(adminId), "Введите новый депозит (число):")
            }
        }
    }

    // Шаг 4: Админ вводит новое значение вместимости
    dispatcher.message(Filter.Text and StateFilter(State.AdminEditingTableCapacity.key)) {
        val adminId = message.from!!.id
        val newCapacity = message.text?.toIntOrNull()
        val tableId = StateStorage.getContext(adminId).editingTableId

        if (newCapacity != null && tableId != null) {
            tableService.updateTableCapacity(tableId, newCapacity)
            TelegramApi.sendMessage(ChatId.fromId(adminId), "Вместимость стола №$tableId обновлена на $newCapacity.")
        } else {
            TelegramApi.sendMessage(ChatId.fromId(adminId), "Ошибка. Введите корректное число.")
        }
        StateStorage.clear(adminId)
    }

    // Шаг 5: Админ вводит новое значение депозита
    dispatcher.message(Filter.Text and StateFilter(State.AdminEditingTableDeposit.key)) {
        val adminId = message.from!!.id
        val newDeposit = message.text?.toBigDecimalOrNull()
        val tableId = StateStorage.getContext(adminId).editingTableId

        if (newDeposit != null && tableId != null) {
            tableService.updateTableDeposit(tableId, newDeposit)
            TelegramApi.sendMessage(ChatId.fromId(adminId), "Депозит стола №$tableId обновлен на $newDeposit.")
        } else {
            TelegramApi.sendMessage(ChatId.fromId(adminId), "Ошибка. Введите корректное число.")
        }
        StateStorage.clear(adminId)
    }
}
