package com.bookingbot.gateway

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.respond
import org.koin.ktor.ext.get
import security.ValidationRules
import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.gateway.waitlist.WaitlistScheduler
import com.bookingbot.gateway.ConfigProvider

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
        global {
            rateLimiter(limit = rateLimitConf.requests, refillPeriod = rateLimitConf.window)
        }
    }

    install(RequestValidation) {
        validate<String> { phone ->
            ValidationRules.validatePhone(phone)
        }
        validate<GuestDTO> { dto ->
            ValidationRules.validateGuestName(dto.name)
        }
        validate<BookingRequest> { br ->
            br.phone?.let { p ->
                val res = ValidationRules.validatePhone(p)
                if (res is ValidationResult.Invalid) return@validate res
            }
            br.bookingGuestName?.let { n ->
                val res = ValidationRules.validateGuestName(n)
                if (res is ValidationResult.Invalid) return@validate res
            }
            ValidationResult.Valid
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
