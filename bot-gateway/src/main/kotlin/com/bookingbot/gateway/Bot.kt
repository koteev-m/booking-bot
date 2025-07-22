package com.bookingbot.gateway

import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.ClubService
import com.bookingbot.api.services.TableService
import com.bookingbot.api.services.UserService
import com.bookingbot.gateway.handlers.addAdminActionHandler
import com.bookingbot.gateway.handlers.addAdminHandlers
import com.bookingbot.gateway.handlers.addAdminTableManagementHandler
import com.bookingbot.gateway.handlers.addAskQuestionHandler
import com.bookingbot.gateway.handlers.addBookingHandlers
import com.bookingbot.gateway.handlers.addClubInfoHandler
import com.bookingbot.gateway.handlers.addContentHandlers
import com.bookingbot.gateway.handlers.addMyBookingsHandler
import com.bookingbot.gateway.handlers.addPromoterHandlers
import com.bookingbot.gateway.handlers.addStartHandler
import com.bookingbot.gateway.handlers.addOwnerHandlers
import com.bookingbot.gateway.handlers.addAdminDashboardHandler
import com.bookingbot.gateway.handlers.addPromoterDashboardHandler
import com.bookingbot.api.services.EventService
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
    // --- Пути к секретам в Docker ---
    private const val TOKEN_SECRET_PATH = "/run/secrets/telegram_bot_mix_token" // <<< ИЗМЕНЕНО
    private const val OWNERS_ID_SECRET_PATH = "/run/secrets/owner_id"
    private const val GENERAL_ADMIN_CHANNEL_ID_SECRET_PATH = "/run/secrets/general_admin_channel_id"

    // --- Загрузка конфигурации ---
    val OWNER_IDS: Set<Long> = readOwnerIds()
    val GENERAL_ADMIN_CHANNEL_ID: Long = readSecretAsLong("GENERAL_ADMIN_CHANNEL_ID", GENERAL_ADMIN_CHANNEL_ID_SECRET_PATH)

    private fun readToken(): String {
        val secretFile = File(TOKEN_SECRET_PATH)
        if (secretFile.exists()) {
            return secretFile.readText().trim()
        }
        // Откат на переменную окружения для локальной разработки
        return System.getenv("TELEGRAM_BOT_TOKEN")
            ?: throw IllegalStateException("Telegram token not found in secret file or environment variable 'TELEGRAM_BOT_TOKEN'")
    }

    private fun readOwnerIds(): Set<Long> {
        val secretFile = File(OWNERS_ID_SECRET_PATH)
        val idsString = if (secretFile.exists()) {
            secretFile.readText().trim()
        } else {
            System.getenv("OWNER_IDS")
        }

        return idsString?.split(',')
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: throw IllegalStateException("Owner IDs not found in secret file or environment variable 'OWNER_IDS'")
    }

    private fun readSecretAsLong(envName: String, path: String): Long {
        val secretFile = File(path)
        val secretValue = if (secretFile.exists()) {
            secretFile.readText().trim()
        } else {
            System.getenv(envName)
        }
        return secretValue?.toLongOrNull()
            ?: throw IllegalStateException("$envName not found in secret file or environment variable")
    }

    // --- Инициализация сервисов ---
    private val userService = UserService()
    private val clubService = ClubService()
    private val tableService = TableService()
    private val bookingService = BookingService()
    private val eventService = EventService()

    // --- Создание экземпляра бота ---
    val instance = bot {
        token = readToken()

        dispatch {
            addStartHandler(this, userService)
            addBookingHandlers(this, clubService, tableService, bookingService)
            addMyBookingsHandler(this, bookingService, clubService)
            addClubInfoHandler(this, clubService, tableService, eventService)
            addAskQuestionHandler(this, clubService)
            addAdminHandlers(this, userService, clubService)
            addPromoterHandlers(this, userService, clubService)
            addAdminActionHandler(this, bookingService, clubService)
            addContentHandlers(this)
            addAdminTableManagementHandler(this, tableService)
            addOwnerHandlers(this, clubService, tableService, eventService)
            addAdminDashboardHandler(this, userService, bookingService)
            addPromoterDashboardHandler(this, bookingService, clubService)
        }
    }
}