package com.bookingbot.gateway

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import com.bookingbot.gateway.waitlist.WaitlistScheduler
import java.time.Duration
import org.koin.ktor.ext.get

fun main() {
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureDI()
    val scheduler = WaitlistScheduler(get(), Duration.ofMinutes(1))
    scheduler.start()
    configureAuth()
    configureMetrics()
    configureRouting()
}
