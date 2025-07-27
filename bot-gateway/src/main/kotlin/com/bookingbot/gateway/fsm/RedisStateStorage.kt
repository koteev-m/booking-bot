package com.bookingbot.gateway.fsm

import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class RedisStateStorage(
    redisUrl: String,
    private val ttlSeconds: Long = 86_400
) : StateStorage {

    private val client = RedisClient.create(redisUrl)
    private val connection = client.connect().coroutines()

    private val mutex = Mutex() // only for connection reauth if needed
    private val contexts = ConcurrentHashMap<Long, BookingContext>()

    override suspend fun saveState(chatId: Long, state: State) {
        val key = "fsm:$chatId"
        connection.setex(key, ttlSeconds, Json.encodeToString(state))
    }

    override suspend fun getState(chatId: Long): State? =
        connection.get("fsm:$chatId")?.let { Json.decodeFromString<State>(it) }

    override suspend fun getContext(chatId: Long): BookingContext =
        mutex.withLock { contexts.getOrPut(chatId) { BookingContext() } }

    override suspend fun clearState(chatId: Long) {
        connection.del("fsm:$chatId")
        mutex.withLock { contexts.remove(chatId) }
    }

    suspend fun ping(): String = connection.ping()

    fun close() = connection.close()
}
