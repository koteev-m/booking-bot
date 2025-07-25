package com.bookingbot.gateway.handlers
import com.bookingbot.gateway.TelegramApi

import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.bookingbot.gateway.util.CallbackData

fun addContentHandlers(dispatcher: Dispatcher) {

    // Обработчик для кнопки "Музыка"
    dispatcher.callbackQuery(CallbackData.MUSIC_SETS) {
        val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)

        // Заглушка: в будущем этот список можно будет брать из БД
        val musicSets = listOf(
            "DJ Proton - Live @ Cosmos (July 2025)" to "${CallbackData.PLAY_SET_PREFIX}1",
            "DJ Neutron - Summer Mix" to "${CallbackData.PLAY_SET_PREFIX}2",
            "DJ Electron - Night Vibes" to "${CallbackData.PLAY_SET_PREFIX}3"
        )

        val musicButtons = musicSets.map { (title, callbackData) ->
            listOf(InlineKeyboardButton.CallbackData(text = title, callbackData = callbackData))
        }

        TelegramApi.sendMessage(
            chatId = chatId,
            text = "🎧 Последние музыкальные сеты от наших резидентов:",
            replyMarkup = InlineKeyboardMarkup.create(musicButtons)
        )
    }

    // Обработчик для выбора конкретного сета
    dispatcher.callbackQuery {
        if (!callbackQuery.data.startsWith(CallbackData.PLAY_SET_PREFIX)) return@callbackQuery

        val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)
        val setId = callbackQuery.data

        // Заглушка со ссылками
        val musicLinks = mapOf(
            "${CallbackData.PLAY_SET_PREFIX}1" to "https://soundcloud.com/example/dj-proton-live",
            "${CallbackData.PLAY_SET_PREFIX}2" to "https://www.youtube.com/watch?v=example2",
            "${CallbackData.PLAY_SET_PREFIX}3" to "https://soundcloud.com/example/dj-electron-vibes"
        )

        val link = musicLinks[setId]
        val text = if (link != null) {
            "🎶 *Приятного прослушивания!*\n\n[Слушать сет]($link)"
        } else {
            "Не удалось найти этот сет."
        }

        TelegramApi.sendMessage(
            chatId = chatId,
            text = text,
            parseMode = ParseMode.MARKDOWN,
            disableWebPagePreview = false
        )
        bot.answerCallbackQuery(callbackQuery.id)
    }
}