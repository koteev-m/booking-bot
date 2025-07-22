package com.bookingbot.gateway.util

/**
 * Вспомогательная функция для экранирования специальных символов,
 * которые используются для форматирования в режиме MarkdownV2 Telegram.
 */
fun String.escapeMarkdownV2(): String {
    return this.replace(Regex("[_\\*\\[\\]()~`>#\\+\\-=|{}.!]")) { "\\${it.value}" }
}