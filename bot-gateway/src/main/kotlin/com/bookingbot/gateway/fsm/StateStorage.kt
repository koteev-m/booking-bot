package com.bookingbot.gateway.fsm

import java.util.concurrent.ConcurrentHashMap

// Потокобезопасное хранилище для состояний и контекста пользователей
object StateStorage {
    private val userStates = ConcurrentHashMap<Long, String>()
    private val userContexts = ConcurrentHashMap<Long, BookingContext>()

    fun setState(userId: Long, state: State) { // <<< Убедитесь, что здесь используется тип State
        userStates[userId] = state.key
    }

    fun getState(userId: Long): String? = userStates[userId]

    fun getContext(userId: Long): BookingContext {
        return userContexts.getOrPut(userId) { BookingContext() }
    }

    fun clear(userId: Long) {
        userStates.remove(userId)
        userContexts.remove(userId)
    }
}
