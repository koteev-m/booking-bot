package com.bookingbot.gateway

import io.ktor.server.application.*
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheus.PrometheusMeterRegistry
import ktor_health_check.Health
import org.koin.ktor.ext.get
import java.util.concurrent.TimeUnit

private data class HealthConf(
    val latencyMaxMs: Long
)

fun Application.configureHealth(promRegistry: PrometheusMeterRegistry = com.bookingbot.gateway.promRegistry) {
    val conf = HealthConf(
        latencyMaxMs = environment.config.property("health.latency.max").getString().toLong()
    )
    install(Health) {
        readyCheck("postgres") { runCatching { DatabaseFactory.exists("bookings") }.getOrDefault(false) }
        readyCheck("redis") { runCatching { get<com.bookingbot.gateway.fsm.RedisStateStorage>().ping() == "PONG" }.getOrDefault(false) }
        readyCheck("latency") {
            val timer = promRegistry.find("http.server.requests").timer()
            val p95 = timer?.percentile(0.95, TimeUnit.MILLISECONDS) ?: Double.NaN
            !p95.isNaN() && p95 < conf.latencyMaxMs
        }
    }
}
