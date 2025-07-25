package com.bookingbot.gateway.handlers
import com.bookingbot.gateway.TelegramApi

import com.bookingbot.api.services.ClubService
import com.bookingbot.api.services.EventService
import com.bookingbot.api.services.TableService
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.bookingbot.gateway.util.CallbackData
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import java.time.format.DateTimeFormatter
import java.time.ZoneId

fun addClubInfoHandler(dispatcher: Dispatcher, clubService: ClubService, tableService: TableService, eventService: EventService) {
    val specificPrefixes = listOf(
        CallbackData.CLUB_INFO_PREFIX,
        CallbackData.CLUB_PHOTOS_PREFIX,
        CallbackData.CLUB_EVENTS_PREFIX
    )

    dispatcher.callbackQuery {
        val data = callbackQuery.data
        if (specificPrefixes.none { data.startsWith(it) }) return@callbackQuery

        val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)

        when {
            data.startsWith(CallbackData.CLUB_INFO_PREFIX) -> {
                val clubId = data.removePrefix(CallbackData.CLUB_INFO_PREFIX).toIntOrNull() ?: return@callbackQuery
                val club = clubService.findClubById(clubId)

                if (club != null) {
                    // <<< НАЧАЛО: Динамическая загрузка столов
                    val tables = tableService.getTablesForClub(clubId)
                    val tablesInfo = if (tables.isNotEmpty()) {
                        tables.joinToString("\n") {
                            "• Стол №${it.number}: до ${it.capacity} чел, депозит от ${it.minDeposit} руб."
                        }
                    } else {
                        "Столы еще не добавлены."
                    }
                    // <<< КОНЕЦ: Динамическая загрузка столов

                    val infoText = """
                        *${club.name}*
                        
                        ${club.description}
                        
                        *Наши столы:*
                        $tablesInfo
                        
                        *Адрес:* ул. Клубная, д. 1
                        *Телефон:* +7 (999) 123-45-67
                    """.trimIndent()

                    TelegramApi.sendMessage(chatId, text = infoText, parseMode = ParseMode.MARKDOWN)
                } else {
                    TelegramApi.sendMessage(chatId, text = "Информация о клубе не найдена.")
                }
            }
            data.startsWith(CallbackData.CLUB_PHOTOS_PREFIX) -> {
                bot.answerCallbackQuery(callbackQuery.id, text = "Раздел 'Фотоотчеты' в разработке.", showAlert = true)
            }
            data.startsWith(CallbackData.CLUB_EVENTS_PREFIX) -> {
                val clubId = data.removePrefix(CallbackData.CLUB_EVENTS_PREFIX).toIntOrNull() ?: return@callbackQuery
                val events = eventService.findUpcomingEventsByClub(clubId)

                if (events.isEmpty()) {
                    TelegramApi.sendMessage(chatId, "В ближайшее время мероприятий не запланировано.")
                    return@callbackQuery
                }

                val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault())
                val eventsText = events.joinToString("\n\n") {
                    "*${formatter.format(it.eventDate)}* - ${it.title}"
                }
                TelegramApi.sendMessage(chatId, "*Ближайшие события:*\n\n$eventsText", parseMode = ParseMode.MARKDOWN)
            }
        }
    }
}