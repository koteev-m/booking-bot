package com.bookingbot.gateway

import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.replymarkup.ReplyMarkup
import com.github.kotlintelegrambot.network.Response
import com.github.kotlintelegrambot.entities.Message
import io.micrometer.core.instrument.Counter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Wrapper around [Bot.instance] providing retry logic and centralized logging
 * for Telegram API calls.
 */
object TelegramApi {
    private const val RETRY_DELAY_MS = 1000L

    private val retryFailures = Counter
        .builder("telegram_api_retry_failures_total")
        .description("Failed retries to Telegram API")
        .register(promRegistry)

    fun sendMessage(
        chatId: ChatId,
        text: String,
        parseMode: ParseMode? = null,
        disableWebPagePreview: Boolean? = null,
        replyMarkup: ReplyMarkup? = null
    ): Response<Message> = runBlocking {
        withRetry {
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
    ): Response<Message> = runBlocking {
        withRetry {
            Bot.instance.forwardMessage(
                chatId = chatId,
                fromChatId = fromChatId,
                messageId = messageId
            )
        }
    }

    /**
     * Executes [block] with retry logic using a non-blocking delay between attempts.
     */
    private suspend fun <T> withRetry(
        attempts: Int = 3,
        delayMs: Long = RETRY_DELAY_MS,
        block: suspend () -> T
    ): T {
        var currentAttempt = 0
        while (true) {
            try {
                return block()
            } catch (e: Exception) {
                retryFailures.increment()
                if (++currentAttempt >= attempts) throw e
                delay(delayMs)
            }
        }
    }
}
