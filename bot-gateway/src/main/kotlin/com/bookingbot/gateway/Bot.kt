package com.bookingbot.gateway

import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.ClubService
import com.bookingbot.api.services.TableService
import com.bookingbot.api.services.UserService
import com.bookingbot.gateway.handlers.*
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
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

    // Создаем экземпляры всех сервисов
    private val userService = UserService()
    private val clubService = ClubService()
    private val tableService = TableService()
    private val bookingService = BookingService()

    val instance = bot {
        token = readToken()

        dispatch {
            // Передаем все необходимые сервисы в обработчики
            addStartHandler(this, userService)
            addBookingHandlers(this, clubService, tableService, bookingService)
            addMyBookingsHandler(this, bookingService)
            addClubInfoHandler(this, clubService)
            addAskQuestionHandler(this, clubService)
            addAdminHandlers(this, userService, clubService)
            addPromoterHandlers(this, userService, clubService)
            addAdminActionHandler(this, bookingService)
        }
    }
}