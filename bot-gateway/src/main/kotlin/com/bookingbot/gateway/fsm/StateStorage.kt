package com.bookingbot.gateway.fsm

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

// Потокобезопасное хранилище для состояний и контекста пользователей.
// Для предотвращения гонок состояний операции для одного пользователя
// защищены корутинным Mutex.
object StateStorage {
    private val userStates = ConcurrentHashMap<Long, String>()
    private val userContexts = ConcurrentHashMap<Long, BookingContext>()
    private val userMutexes = ConcurrentHashMap<Long, Mutex>()

    private fun mutex(userId: Long): Mutex =
        userMutexes.getOrPut(userId) { Mutex() }

    fun setState(userId: Long, state: State) = runBlocking {
        mutex(userId).withLock {
            userStates[userId] = state.key
        }
    }

    fun getState(userId: Long): String? = runBlocking {
        mutex(userId).withLock { userStates[userId] }
    }

    fun getContext(userId: Long): BookingContext = runBlocking {
        mutex(userId).withLock { userContexts.getOrPut(userId) { BookingContext() } }
    }

    fun clear(userId: Long) = runBlocking {
        mutex(userId).withLock {
            userStates.remove(userId)
            userContexts.remove(userId)
            userMutexes.remove(userId)
        }
    }
}
