package com.bookingbot.gateway.handlers

import com.bookingbot.api.services.ClubService
import com.bookingbot.api.services.EventService
import com.bookingbot.api.services.TableService
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import java.time.format.DateTimeFormatter
import java.time.ZoneId

fun addClubInfoHandler(dispatcher: Dispatcher, clubService: ClubService, tableService: TableService, eventService: EventService) {
    val specificPrefixes = listOf("club_info_", "club_photos_", "club_events_")

    dispatcher.callbackQuery {
        val data = callbackQuery.data
        if (specificPrefixes.none { data.startsWith(it) }) return@callbackQuery

        val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)

        when {
            data.startsWith("club_info_") -> {
                val clubId = data.removePrefix("club_info_").toIntOrNull() ?: return@callbackQuery
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

                    bot.sendMessage(chatId, text = infoText, parseMode = ParseMode.MARKDOWN)
                } else {
                    bot.sendMessage(chatId, text = "Информация о клубе не найдена.")
                }
            }
            data.startsWith("club_photos_") -> {
                bot.answerCallbackQuery(callbackQuery.id, text = "Раздел 'Фотоотчеты' в разработке.", showAlert = true)
            }
            data.startsWith("club_events_") -> {
                val clubId = data.removePrefix("club_events_").toIntOrNull() ?: return@callbackQuery
                val events = eventService.findUpcomingEventsByClub(clubId)

                if (events.isEmpty()) {
                    bot.sendMessage(chatId, "В ближайшее время мероприятий не запланировано.")
                    return@callbackQuery
                }

                val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault())
                val eventsText = events.joinToString("\n\n") {
                    "*${formatter.format(it.eventDate)}* - ${it.title}"
                }
                bot.sendMessage(chatId, "*Ближайшие события:*\n\n$eventsText", parseMode = ParseMode.MARKDOWN)
            }
        }
    }
}