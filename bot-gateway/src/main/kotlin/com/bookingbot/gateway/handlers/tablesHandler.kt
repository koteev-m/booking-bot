package com.bookingbot.gateway.handlers

import com.bookingbot.api.services.BookingService
import com.bookingbot.gateway.sendHallScheme
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command

/** Registers /tables command showing hall scheme with free tables. */
fun addTablesHandler(dispatcher: Dispatcher, bookingService: BookingService) {
    dispatcher.command("tables") {
        val chatId = message.chat.id
        val freeTables = bookingService.getFreeTables()
        bot.sendHallScheme(chatId, freeTables)
    }
}
