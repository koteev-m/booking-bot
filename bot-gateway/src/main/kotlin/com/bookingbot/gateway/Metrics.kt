package com.bookingbot.gateway

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.time.Duration

val promRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun Application.configureMetrics() {
    install(MicrometerMetrics) {
        registry = promRegistry
        distributionStatisticConfig = DistributionStatisticConfig.DEFAULT.merge(
            DistributionStatisticConfig.builder()
                .percentilesHistogram(true)
                .percentiles(0.95, 0.99)
                .serviceLevelObjectives(
                    Duration.ofMillis(50),
                    Duration.ofMillis(100),
                    Duration.ofMillis(200),
                    Duration.ofMillis(500),
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(2)
                )
                .build()
        )
    }
    routing {
        get("/metrics") { call.respondText(promRegistry.scrape()) }
    }
    environment.config.propertyOrNull("monitoring.prometheus.port")?.getString()?.toInt()?.let { port ->
        embeddedServer(Netty, port = port, host = "0.0.0.0") {
            routing {
                get("/metrics") { call.respondText(promRegistry.scrape()) }
            }
        }.start(wait = false)
    }
}
