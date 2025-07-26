package com.bookingbot.gateway.fsm

import redis.embedded.RedisServer
import redis.embedded.RedisServerBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StateStorageTest {
    private lateinit var server: RedisServer
    private lateinit var storage: RedisStateStorage
    private val port = 6379

    @BeforeEach
    fun setUp() {
        server = RedisServerBuilder().port(port).build()
        server.start()
        storage = RedisStateStorage("redis://localhost:$port/0", ttlSeconds = 1)
    }

    @AfterEach
    fun tearDown() {
        storage.close()
        server.stop()
    }

    @Test
    fun saveGetClearRoundTrip() = runTest {
        storage.saveState(1L, State.ClubSelection)
        assertEquals(State.ClubSelection, storage.getState(1L))
        storage.clearState(1L)
        assertNull(storage.getState(1L))
    }

    @Test
    fun ttlExpires() = runTest {
        storage.saveState(2L, State.DateSelection)
        delay(1500)
        assertNull(storage.getState(2L))
    }
}
