package com.bookingbot.gateway.waitlist

import com.bookingbot.api.services.BookingService
import io.ktor.server.application.*
import kotlinx.coroutines.*
import java.time.Duration

/**
 * Periodically processes wait-list entries.
 *
 * Starts a repeating coroutine on application start and
 * cancels it on ApplicationStopped to avoid leaks.
 */
class WaitlistScheduler(
    private val bookingService: BookingService,
    private val period: Duration = Duration.ofMinutes(1)
) {
    private lateinit var job: Job
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun Application.start() {
        job = scope.launch {
            while (isActive) {
                bookingService.processWaitlist()   // existing business logic
                delay(period.toMillis())
            }
        }
        environment.monitor.subscribe(ApplicationStopped) { job.cancel() }
    }
}
