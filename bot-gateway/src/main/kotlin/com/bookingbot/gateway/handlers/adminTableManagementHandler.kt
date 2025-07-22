package com.bookingbot.gateway.handlers

import com.bookingbot.api.services.TableService
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

fun addAdminTableManagementHandler(dispatcher: Dispatcher, tableService: TableService) {
    // Шаг 1: Админ нажимает "Управление столами"
    dispatcher.callbackQuery("admin_manage_tables") {
        val adminId = callbackQuery.from.id
        // TODO: Определить clubId для админа
        val clubId = 1 // Заглушка
        val tables = tableService.getTablesForClub(clubId)
        val tableButtons = tables.map {
            InlineKeyboardButton.CallbackData("Стол №${it.number} (вмест: ${it.capacity}, деп: ${it.minDeposit})", "admin_edit_table_${it.id}")
        }.chunked(1)

        StateStorage.setState(adminId, State.AdminSelectTableToEdit)
        bot.sendMessage(ChatId.fromId(adminId), "Выберите стол для редактирования:", replyMarkup = InlineKeyboardMarkup.create(tableButtons))
    }

    // Шаг 2: Админ выбрал стол, спрашиваем, что редактировать
    dispatcher.callbackQuery {
        if (!callbackQuery.data.startsWith("admin_edit_table_")) return@callbackQuery
        val adminId = callbackQuery.from.id
        if (StateStorage.getState(adminId) != State.AdminSelectTableToEdit.key) return@callbackQuery

        val tableId = callbackQuery.data.removePrefix("admin_edit_table_").toInt()
        StateStorage.getContext(adminId).editingTableId = tableId

        val editOptions = InlineKeyboardMarkup.create(listOf(
            InlineKeyboardButton.CallbackData("Вместимость", "edit_capacity"),
            InlineKeyboardButton.CallbackData("Депозит", "edit_deposit")
        ))
        bot.editMessageText(ChatId.fromId(adminId), callbackQuery.message!!.messageId, text = "Что вы хотите изменить для стола №$tableId?", replyMarkup = editOptions)
    }

    // Шаг 3: Админ выбрал "Вместимость" или "Депозит"
    dispatcher.callbackQuery {
        val adminId = callbackQuery.from.id
        when(callbackQuery.data) {
            "edit_capacity" -> {
                StateStorage.setState(adminId, State.AdminEditingTableCapacity)
                bot.sendMessage(ChatId.fromId(adminId), "Введите новую вместимость (число):")
            }
            "edit_deposit" -> {
                StateStorage.setState(adminId, State.AdminEditingTableDeposit)
                bot.sendMessage(ChatId.fromId(adminId), "Введите новый депозит (число):")
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
            bot.sendMessage(ChatId.fromId(adminId), "Вместимость стола №$tableId обновлена на $newCapacity.")
        } else {
            bot.sendMessage(ChatId.fromId(adminId), "Ошибка. Введите корректное число.")
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
            bot.sendMessage(ChatId.fromId(adminId), "Депозит стола №$tableId обновлен на $newDeposit.")
        } else {
            bot.sendMessage(ChatId.fromId(adminId), "Ошибка. Введите корректное число.")
        }
        StateStorage.clear(adminId)
    }
}
