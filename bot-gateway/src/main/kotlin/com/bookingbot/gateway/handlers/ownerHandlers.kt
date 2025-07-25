package com.bookingbot.gateway.handlers
import com.bookingbot.gateway.TelegramApi

import com.bookingbot.api.services.ClubService
import com.bookingbot.api.services.TableService
import com.bookingbot.gateway.Bot.OWNER_IDS
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.bookingbot.api.services.EventService
import com.bookingbot.gateway.Bot
import java.time.LocalDate
import java.time.ZoneId

fun addOwnerHandlers(dispatcher: Dispatcher, clubService: ClubService, tableService: TableService, eventService: EventService) {

    // Команда для создания нового клуба
    dispatcher.command("addclub") {
        if (message.from?.id !in OWNER_IDS) return@command

        val clubName = args.joinToString(" ")
        if (clubName.isBlank()) {
            TelegramApi.sendMessage(ChatId.fromId(message.chat.id), "Укажите название клуба: `/addclub <название>`", parseMode = ParseMode.MARKDOWN)
            return@command
        }

        val newClub = clubService.createClub(clubName, "Описание для ${clubName}")
        TelegramApi.sendMessage(ChatId.fromId(message.chat.id), "✅ Клуб '${newClub.name}' успешно создан с ID: ${newClub.id}")
    }

    // Команда для установки канала администраторов
    dispatcher.command("setclubchannel") {
        if (message.from?.id !in OWNER_IDS) return@command

        if (args.size != 2) {
            TelegramApi.sendMessage(ChatId.fromId(message.chat.id), "Формат: `/setclubchannel <ID клуба> <ID канала>`", parseMode = ParseMode.MARKDOWN)
            return@command
        }
        val clubId = args[0].toIntOrNull()
        val channelId = args[1].toLongOrNull()

        if (clubId == null || channelId == null) {
            TelegramApi.sendMessage(ChatId.fromId(message.chat.id), "Неверный формат ID.")
            return@command
        }

        if (clubService.setAdminChannel(clubId, channelId)) {
            TelegramApi.sendMessage(ChatId.fromId(message.chat.id), "✅ Канал $channelId успешно назначен для клуба $clubId.")
        } else {
            TelegramApi.sendMessage(ChatId.fromId(message.chat.id), "❌ Не удалось найти клуб с ID $clubId.")
        }
    }

    // Команда для добавления нового стола
    dispatcher.command("addtable") {
        if (message.from?.id !in OWNER_IDS) return@command

        if (args.size != 4) {
            TelegramApi.sendMessage(ChatId.fromId(message.chat.id), "Формат: `/addtable <ID клуба> <номер стола> <вместимость> <депозит>`", parseMode = ParseMode.MARKDOWN)
            return@command
        }
        val clubId = args[0].toIntOrNull()
        val tableNumber = args[1].toIntOrNull()
        val capacity = args[2].toIntOrNull()
        val deposit = args[3].toBigDecimalOrNull()

        if (clubId == null || tableNumber == null || capacity == null || deposit == null) {
            TelegramApi.sendMessage(ChatId.fromId(message.chat.id), "Неверный формат данных.")
            return@command
        }

        val newTable = tableService.createTable(clubId, tableNumber, capacity, deposit)
        TelegramApi.sendMessage(ChatId.fromId(message.chat.id), "✅ Стол №${newTable.number} добавлен в клуб $clubId.")
    }

    // Команда для добавления нового события/афиши
    dispatcher.command("addevent") {
        if (message.from?.id !in Bot.OWNER_IDS) return@command

        // Формат: /addevent <ID клуба> <ДД.ММ.ГГГГ> <Заголовок>
        if (args.size < 3) {
            TelegramApi.sendMessage(ChatId.fromId(message.chat.id), "Формат: `/addevent <ID клуба> <ДД.ММ.ГГГГ> <Заголовок>`", parseMode = ParseMode.MARKDOWN)
            return@command
        }
        val clubId = args[0].toIntOrNull()
        val date = try { LocalDate.parse(args[1], java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) } catch (e: Exception) { null }
        val title = args.drop(2).joinToString(" ")

        if (clubId == null || date == null || title.isBlank()) {
            TelegramApi.sendMessage(ChatId.fromId(message.chat.id), "Неверный формат данных.")
            return@command
        }

        // Опционально: можно добавить запрос на описание и картинку в FSM
        eventService.createEvent(clubId, title, "Описание скоро будет...", date.atStartOfDay(ZoneId.systemDefault()).toInstant(), null)
        TelegramApi.sendMessage(ChatId.fromId(message.chat.id), "✅ Новое событие '$title' добавлено для клуба $clubId.")
    }
}

