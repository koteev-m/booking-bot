package com.bookingbot.gateway.fsm

import java.util.concurrent.ConcurrentHashMap

// Заменит простую userStates map. Потокобезопасное хранилище.
object StateStorage {
    private val userStates = ConcurrentHashMap<Long, String>()
    private val userContexts = ConcurrentHashMap<Long, BookingContext>()

    fun setState(userId: Long, state: BookingState) {
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