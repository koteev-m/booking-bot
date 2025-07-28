package com.bookingbot.gateway.handlers.booking

import com.bookingbot.api.services.BookingService
import com.bookingbot.gateway.hall.HallSchemeRenderer
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.files.TelegramFile
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Registers /tables command showing hall scheme with free tables. */
object ListTablesHandler : KoinComponent {
    private val bookingService: BookingService by inject()
    private val renderer: HallSchemeRenderer by inject()

    fun register(dispatcher: Dispatcher) {
        dispatcher.command("tables") {
            val chatId = ChatId.fromId(message.chat.id)
            val freeTables = bookingService.getFreeTables()
            val bytes = renderer.render(freeTables)
            bot.sendPhoto(chatId, TelegramFile.ByByteArray(bytes, "hall.png"))
        }
    }
}
