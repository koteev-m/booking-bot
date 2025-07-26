package com.bookingbot.gateway

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import com.github.kotlintelegrambot.entities.files.TelegramFile
import com.github.kotlintelegrambot.network.TelegramApiError
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Utilities for sending multiple photos as a swipeable album.
 */
private val logger = LoggerFactory.getLogger("PhotoReport")

/**
 * Send a group of photos as an album. The caption is applied only to the first image.
 *
 * @param chatId target chat identifier.
 * @param photoSources file paths or URLs of images.
 * @param title optional caption shown once for the album.
 */
fun Bot.sendPhotoReport(
    chatId: Long,
    photoSources: List<String>,
    title: String? = null
) {
    try {
        val mediaList = photoSources.mapIndexed { index, src ->
            val file = File(src)
            val input = TelegramFile.ByFile(file).takeIf { file.exists() }
                ?: TelegramFile.ByUrl(src)
            InputMediaPhoto(
                media = input,
                caption = if (index == 0) title else null,
                parseMode = ParseMode.MARKDOWN
            )
        }
        sendMediaGroup(ChatId.fromId(chatId), mediaList)
    } catch (e: TelegramApiError) {
        logger.error("Failed to send photo report", e)
    }
}

/**
 * Example command handler that sends a daily photo report.
 */
fun addPhotoReportHandler(dispatcher: Dispatcher) {
    dispatcher.command("report") {
        val photos = listOf(
            "photos/report1.jpg",
            "photos/report2.jpg",
            "photos/report3.jpg"
        )
        bot.sendPhotoReport(message.chat.id, photos, "Daily report")
    }
}
