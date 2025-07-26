package com.bookingbot.gateway.waitlist

import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.api.model.waitlist.WaitEntry
import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.TableService
import com.bookingbot.api.services.WaitlistDao
import com.bookingbot.api.services.WaitlistNotifier
import com.bookingbot.api.services.WaitlistNotifierHolder
import com.bookingbot.api.tables.TablesTable
import com.bookingbot.api.services.BookingRepository
import com.bookingbot.gateway.ApplicationScope
import com.bookingbot.gateway.Bot
import com.bookingbot.gateway.TelegramApi
import com.bookingbot.gateway.util.CallbackData
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Waiting list feature implementation.
 */
class WaitlistNotifierImpl(
    private val bookingService: BookingService,
    private val tableService: TableService
) : WaitlistNotifier {
    private val logger = LoggerFactory.getLogger(WaitlistNotifierImpl::class.java)

    override fun onNewBooking() { ApplicationScope.launch { scanWaitlist() } }
    override fun onCancel() { ApplicationScope.launch { scanWaitlist() } }

    suspend fun scanWaitlist() = coroutineScope {
        val entries = WaitlistDao.findActive()
        entries.groupBy { it.desiredTime }.forEach { (time, group) ->
            val localTime = LocalDateTime.ofInstant(time, ZoneId.systemDefault())
            val freeMap = BookingRepository.findFreeTables(localTime)
            val anyFree = freeMap.anyTrue()
            val firstFree = freeMap.entries.firstOrNull { it.value }?.key
            group.forEach { entry ->
                val tableId = entry.preferredTable ?: firstFree
                if (tableId != null && freeMap[tableId] == true) {
                    notifyGuest(entry, tableId)
                } else if (entry.preferredTable == null && anyFree && firstFree != null) {
                    notifyGuest(entry, firstFree)
                }
            }
        }
    }

    private fun isTableFree(tableId: Int, time: Instant): Boolean =
        BookingRepository
            .findFreeTables(LocalDateTime.ofInstant(time, ZoneId.systemDefault()))[tableId] == true

    private fun findAnyFreeTable(time: Instant): Int? =
        BookingRepository
            .findFreeTables(LocalDateTime.ofInstant(time, ZoneId.systemDefault()))
            .entries
            .firstOrNull { it.value }
            ?.key

    private fun notifyGuest(entry: WaitEntry, tableId: Int) {
        val timeText = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(entry.desiredTime)
        val msg = "Table #$tableId at $timeText is now free. Move your booking? /accept_${entry.id} /skip_${entry.id}"
        try {
            TelegramApi.sendMessage(ChatId.fromId(entry.chatId), msg)
            WaitlistDao.updateStatus(entry.id, "OFFERED")
        } catch (e: Exception) {
            logger.error("Failed to notify user", e)
        }
    }
}

private fun Map<Int, Boolean>.anyTrue() = values.any { it }

/** Offer user to join waitlist. */
fun Bot.offerWaitlist(chatId: Long, desiredTime: Instant, preferredTable: Int? = null) {
    WaitlistDao.addEntry(chatId, desiredTime, preferredTable)
    TelegramApi.sendMessage(ChatId.fromId(chatId), "Вы добавлены в лист ожидания.")
}

/** Register handlers and example /book command. */
fun addWaitlistHandlers(dispatcher: Dispatcher, bookingService: BookingService, tableService: TableService) {
    val notifier = WaitlistNotifierImpl(bookingService, tableService)
    WaitlistNotifierHolder.notifier = notifier

    dispatcher.command("book") {
        val chatId = message.chat.id
        val preferredTable = args.firstOrNull()?.toIntOrNull()
        val desiredTime = Instant.now().plusSeconds(3600) // demo: one hour later

        if (preferredTable != null && notifier.isTableFree(preferredTable, desiredTime)) {
            bookingService.createBooking(
                BookingRequest(
                    userId = chatId,
                    clubId = TablesTable.select { TablesTable.id eq preferredTable }
                        .single()[TablesTable.clubId],
                    tableId = preferredTable,
                    bookingTime = desiredTime,
                    partySize = 2,
                    expectedDuration = 120,
                    bookingGuestName = null,
                    telegramId = chatId,
                    phone = null,
                    bookingSource = "Бот"
                )
            )
            TelegramApi.sendMessage(ChatId.fromId(chatId), "Бронь создана")
            notifier.onNewBooking()
        } else {
            Bot.offerWaitlist(chatId, desiredTime, preferredTable)
        }
    }

    dispatcher.command("accept") {
        val id = message.text?.substringAfter("_")?.toIntOrNull() ?: return@command
        val entry = WaitlistDao.getEntry(id) ?: return@command
        val tableId = entry.preferredTable ?: notifier.findAnyFreeTable(entry.desiredTime) ?: return@command
        bookingService.createBooking(
            BookingRequest(
                userId = entry.chatId,
                clubId = TablesTable.select { TablesTable.id eq tableId }.single()[TablesTable.clubId],
                tableId = tableId,
                bookingTime = entry.desiredTime,
                partySize = 2,
                expectedDuration = 120,
                bookingGuestName = null,
                telegramId = entry.chatId,
                phone = null,
                bookingSource = "Бот"
            )
        )
        WaitlistDao.updateStatus(id, "FULFILLED")
        TelegramApi.sendMessage(ChatId.fromId(entry.chatId), "Бронь обновлена")
        notifier.onNewBooking()
    }

    dispatcher.command("skip") {
        val id = message.text?.substringAfter("_")?.toIntOrNull() ?: return@command
        WaitlistDao.updateStatus(id, "CANCELLED")
        TelegramApi.sendMessage(ChatId.fromId(message.chat.id), "Запрос отменён")
    }
}
