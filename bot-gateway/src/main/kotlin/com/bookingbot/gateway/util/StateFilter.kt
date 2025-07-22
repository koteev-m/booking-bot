package com.bookingbot.gateway.util

import com.bookingbot.gateway.fsm.StateStorage
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.extensions.filters.Filter

// Этот класс-фильтр проверяет текущее состояние пользователя
class StateFilter(private val state: String) : Filter {
    // Интерфейс Filter требует реализации только одного метода - predicate() для Message
    override fun Message.predicate(): Boolean {
        return StateStorage.getState(chat.id) == state
    }
}