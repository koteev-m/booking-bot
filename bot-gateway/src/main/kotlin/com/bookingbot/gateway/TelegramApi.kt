package com.bookingbot.gateway

import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.replymarkup.ReplyMarkup
import com.github.kotlintelegrambot.network.Response
import com.github.kotlintelegrambot.entities.Message
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import io.github.reugn.kotlin.backoff.ExponentialBackoff

/**
 * Wrapper around [Bot.instance] providing retry logic and centralized logging
 * for Telegram API calls.
 */
object TelegramApi {
    private val config = BackoffConfig.load()

    /** Meter registry used for metrics, overridable for tests. */
    internal var meterRegistry: MeterRegistry = promRegistry

    private fun retryFailures() = Counter
        .builder("telegram_api_retry_failures_total")
        .description("Failed retries to Telegram API")
        .register(meterRegistry)

    suspend fun <T> callTelegram(
        apiMethod: String,
        block: suspend () -> T
    ): T {
        val counterSuccess = Counter
            .builder("telegram_api_requests_total")
            .description("Total successful Telegram API calls")
            .tag("method", apiMethod)
            .tag("status", "success")
            .register(meterRegistry)

        val counterError = Counter
            .builder("telegram_api_requests_total")
            .description("Total failed Telegram API calls")
            .tag("method", apiMethod)
            .tag("status", "error")
            .register(meterRegistry)

        val timer = Timer
            .builder("telegram_api_latency_seconds")
            .description("Latency of Telegram API calls")
            .tag("method", apiMethod)
            .publishPercentiles(0.5, 0.9, 0.99)
            .sla(Duration.ofMillis(500), Duration.ofSeconds(1))
            .register(meterRegistry)

        val start = System.nanoTime()
        try {
            val result = withBackoffRetry { block() }
            counterSuccess.increment()
            return result
        } catch (e: Exception) {
            counterError.increment()
            throw e
        } finally {
            val elapsed = System.nanoTime() - start
            timer.record(elapsed, TimeUnit.NANOSECONDS)
        }
    }

    fun sendMessage(
        chatId: ChatId,
        text: String,
        parseMode: ParseMode? = null,
        disableWebPagePreview: Boolean? = null,
        replyMarkup: ReplyMarkup? = null
    ): Response<Message> = runBlocking {
        callTelegram("sendMessage") {
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
        callTelegram("forwardMessage") {
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
    inline suspend fun <T> withBackoffRetry(
        block: suspend () -> T
    ): T {
        val backoff = ExponentialBackoff(
            config.initialDelayMs,
            config.factor,
            config.maxDelayMs,
            config.jitterPct
        )
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                retryFailures().increment()
                if (++attempt >= config.maxAttempts) throw e
                delay(backoff.nextDelay())
            }
        }
    }
}
