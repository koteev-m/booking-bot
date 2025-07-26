package com.bookingbot.gateway.fsm

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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

object StateStorageImpl : StateStorage, KoinComponent {
    private val delegate: StateStorage by inject()

    override suspend fun saveState(chatId: Long, state: State) =
        delegate.saveState(chatId, state)

    override suspend fun getState(chatId: Long): State? =
        delegate.getState(chatId)

    override suspend fun getContext(chatId: Long): BookingContext =
        delegate.getContext(chatId)

    override suspend fun clearState(chatId: Long) =
        delegate.clearState(chatId)
}
