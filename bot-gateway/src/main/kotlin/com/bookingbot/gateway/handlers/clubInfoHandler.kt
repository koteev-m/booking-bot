package com.bookingbot.gateway.handlers

import com.bookingbot.api.services.ClubService
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode

fun addClubInfoHandler(dispatcher: Dispatcher, clubService: ClubService) {
    // Этот обработчик теперь должен быть более конкретным, чтобы не перехватывать
    // callback'и от других меню (например, календаря)
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
                    val infoText = """
                        *${club.name}*
                        
                        ${club.description}
                        
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
                bot.answerCallbackQuery(callbackQuery.id, text = "Раздел 'Афиши' в разработке.", showAlert = true)
            }
        }
    }
}