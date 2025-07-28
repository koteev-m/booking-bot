package com.bookingbot.gateway

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitExceededException
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import security.ValidationRules
import com.bookingbot.gateway.GuestDTO
import com.bookingbot.gateway.RateLimitConfig
import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.gateway.waitlist.WaitlistScheduler
import com.bookingbot.gateway.ConfigProvider
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
    val scheduler = WaitlistScheduler(get(), ConfigProvider.botConfig.waitlist.periodMs)
    scheduler.start()
    configureAuth()
    val rateLimitConf = RateLimitConfig.load()
    install(RateLimit) {
        global { limit(window = rateLimitConf.window, maxRequests = rateLimitConf.requests) }
    }
    install(RequestValidation) {
        validate<String> { ValidationRules.validatePhone(it) }
        validate<GuestDTO> { ValidationRules.validateGuestName(it.name) }
        validate<BookingRequest> {
            it.phone?.let { p -> ValidationRules.validatePhone(p) }
            it.bookingGuestName?.let { n -> ValidationRules.validateGuestName(n) }
        }
    }
    install(StatusPages) {
        exception<RequestValidationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<RateLimitExceededException> { call, _ ->
            call.respond(HttpStatusCode.TooManyRequests)
        }
    }
    configureMetrics()
    configureRouting()
}
