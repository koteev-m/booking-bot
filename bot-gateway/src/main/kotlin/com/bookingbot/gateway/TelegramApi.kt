package com.bookingbot.gateway

import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.replymarkup.ReplyMarkup
import com.github.kotlintelegrambot.network.Response
import com.github.kotlintelegrambot.entities.Message
import org.slf4j.LoggerFactory

/**
 * Wrapper around [Bot.instance] providing retry logic and centralized logging
 * for Telegram API calls.
 */
object TelegramApi {
    private val logger = LoggerFactory.getLogger(TelegramApi::class.java)
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY_MS = 1000L

    fun sendMessage(
        chatId: ChatId,
        text: String,
        parseMode: ParseMode? = null,
        disableWebPagePreview: Boolean? = null,
        replyMarkup: ReplyMarkup? = null
    ): Response<Message> {
        return withRetry {
            Bot.instance.sendMessage(
                chatId = chatId,
                text = text,
                parseMode = parseMode,
                disableWebPagePreview = disableWebPagePreview,
                replyMarkup = replyMarkup
            )
        }
    }

    fun forwardMessage(
        chatId: ChatId,
        fromChatId: ChatId,
        messageId: Long
    ): Response<Message> {
        return withRetry {
            Bot.instance.forwardMessage(
                chatId = chatId,
                fromChatId = fromChatId,
                messageId = messageId
            )
        }
    }

    private fun <T> withRetry(block: () -> Response<T>): Response<T> {
        var lastError: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                val result = block()
                if (result.isSuccess) {
                    return result
                }
                logger.error(
                    "Telegram API error: {}",
                    result.getError()?.description
                )
            } catch (e: Exception) {
                lastError = e
                logger.warn("Telegram API call failed on attempt ${attempt + 1}", e)
            }
            Thread.sleep(RETRY_DELAY_MS)
        }
        logger.error("Telegram API call failed after $MAX_RETRIES attempts", lastError)
        throw lastError ?: RuntimeException("Unknown Telegram API error")
    }
}
