package com.bookingbot.gateway.fsm

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides thread-safe, non-blocking access to user FSM state and context.
 */
interface StateStorage {
    /**
     * Saves the [state] for the specified [chatId] without blocking the caller.
     */
    suspend fun saveState(chatId: Long, state: State)

    /**
     * Returns the stored state for [chatId] or `null` if none, without blocking.
     */
    suspend fun getState(chatId: Long): State?

    /**
     * Returns a mutable context for [chatId], creating it if necessary.
     * All access is synchronized and non-blocking.
     */
    suspend fun getContext(chatId: Long): BookingContext

    /**
     * Clears state and context for [chatId] in a non-blocking manner.
     */
    suspend fun clearState(chatId: Long)
}

/**
 * In-memory implementation of [StateStorage] using a single [Mutex] to
 * synchronize access to maps.
 */
object StateStorageImpl : StateStorage {
    private val userStates = ConcurrentHashMap<Long, State>()
    private val userContexts = ConcurrentHashMap<Long, BookingContext>()
    private val mutex = Mutex()

    override suspend fun saveState(chatId: Long, state: State) {
        mutex.withLock {
            userStates[chatId] = state
        }
    }

    override suspend fun getState(chatId: Long): State? =
        mutex.withLock { userStates[chatId] }

    override suspend fun getContext(chatId: Long): BookingContext =
        mutex.withLock { userContexts.getOrPut(chatId) { BookingContext() } }

    override suspend fun clearState(chatId: Long) {
        mutex.withLock {
            userStates.remove(chatId)
            userContexts.remove(chatId)
        }
    }
}
