package com.bookingbot.gateway.handlers

import com.bookingbot.api.services.ClubService
import com.bookingbot.api.services.TableService
import com.bookingbot.gateway.Bot.OWNER_IDS
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode

fun addOwnerHandlers(dispatcher: Dispatcher, clubService: ClubService, tableService: TableService) {

    // Команда для создания нового клуба
    dispatcher.command("addclub") {
        if (message.from?.id !in OWNER_IDS) return@command

        val clubName = args.joinToString(" ")
        if (clubName.isBlank()) {
            bot.sendMessage(ChatId.fromId(message.chat.id), "Укажите название клуба: `/addclub <название>`", parseMode = ParseMode.MARKDOWN)
            return@command
        }

        val newClub = clubService.createClub(clubName, "Описание для ${clubName}")
        bot.sendMessage(ChatId.fromId(message.chat.id), "✅ Клуб '${newClub.name}' успешно создан с ID: ${newClub.id}")
    }

    // Команда для установки канала администраторов
    dispatcher.command("setclubchannel") {
        if (message.from?.id !in OWNER_IDS) return@command

        if (args.size != 2) {
            bot.sendMessage(ChatId.fromId(message.chat.id), "Формат: `/setclubchannel <ID клуба> <ID канала>`", parseMode = ParseMode.MARKDOWN)
            return@command
        }
        val clubId = args[0].toIntOrNull()
        val channelId = args[1].toLongOrNull()

        if (clubId == null || channelId == null) {
            bot.sendMessage(ChatId.fromId(message.chat.id), "Неверный формат ID.")
            return@command
        }

        if (clubService.setAdminChannel(clubId, channelId)) {
            bot.sendMessage(ChatId.fromId(message.chat.id), "✅ Канал $channelId успешно назначен для клуба $clubId.")
        } else {
            bot.sendMessage(ChatId.fromId(message.chat.id), "❌ Не удалось найти клуб с ID $clubId.")
        }
    }

    // Команда для добавления нового стола
    dispatcher.command("addtable") {
        if (message.from?.id !in OWNER_IDS) return@command

        if (args.size != 4) {
            bot.sendMessage(ChatId.fromId(message.chat.id), "Формат: `/addtable <ID клуба> <номер стола> <вместимость> <депозит>`", parseMode = ParseMode.MARKDOWN)
            return@command
        }
        val clubId = args[0].toIntOrNull()
        val tableNumber = args[1].toIntOrNull()
        val capacity = args[2].toIntOrNull()
        val deposit = args[3].toBigDecimalOrNull()

        if (clubId == null || tableNumber == null || capacity == null || deposit == null) {
            bot.sendMessage(ChatId.fromId(message.chat.id), "Неверный формат данных.")
            return@command
        }

        val newTable = tableService.createTable(clubId, tableNumber, capacity, deposit)
        bot.sendMessage(ChatId.fromId(message.chat.id), "✅ Стол №${newTable.number} добавлен в клуб $clubId.")
    }
}
