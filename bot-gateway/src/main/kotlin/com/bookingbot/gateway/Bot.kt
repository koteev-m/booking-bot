// bot-gateway/src/main/kotlin/com/bookingbot/gateway/Bot.kt
package com.bookingbot.gateway

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun startTelegramBot() {
    CoroutineScope(Dispatchers.IO).launch {
        Bot.instance.startPolling()
        println("Telegram Bot is listening for updates...")
    }
}

object Bot {
    private const val SECRET_PATH = "/run/secrets/telegram_bot_token"

    private fun readToken(): String {
        val secretFile = File(SECRET_PATH)
        if (secretFile.exists()) {
            return secretFile.readText().trim()
        }
        return System.getenv("TELEGRAM_BOT_TOKEN")
            ?: throw IllegalStateException("Telegram token not found in secret file or environment variable")
    }

    val instance = bot {
        token = readToken()

        dispatch {
            command("start") {
                val mainMenu = KeyboardReplyMarkup(
                    keyboard = listOf(
                        listOf(KeyboardButton("Выбрать клуб"), KeyboardButton("Мои бронирования")),
                        listOf(KeyboardButton("Задать вопрос"), KeyboardButton("Музыка"))
                    ),
                    resizeKeyboard = true
                )
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Добро пожаловать в бот для бронирования столов! 👋\n\nВыберите действие:",
                    replyMarkup = mainMenu
                )
            }
        }
    }
}