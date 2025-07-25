package com.bookingbot.gateway

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * Application-wide CoroutineScope used instead of GlobalScope.
 */
object ApplicationScope : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    /** Cancel all coroutines launched in [ApplicationScope]. */
    fun cancel() {
        job.cancel()
    }
}
