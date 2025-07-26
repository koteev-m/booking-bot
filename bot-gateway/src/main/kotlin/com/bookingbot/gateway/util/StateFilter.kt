package com.bookingbot.gateway.util

import com.bookingbot.gateway.fsm.State
import com.bookingbot.gateway.fsm.StateStorageImpl
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.extensions.filters.Filter

// Этот класс-фильтр проверяет текущее состояние пользователя
class StateFilter(private val state: State) : Filter {
    // Интерфейс Filter требует реализации только одного метода - predicate() для Message
    override fun Message.predicate(): Boolean {
        return StateStorageImpl.getState(chat.id) == state
    }
}