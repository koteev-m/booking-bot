package com.bookingbot.gateway

import GuestDTO
import com.apple.eawt.Application
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitConfig as KtorRateLimitConfig
import io.ktor.server.plugins.ratelimit.RateLimitExceededException
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.koin.ktor.ext.get
import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.gateway.waitlist.WaitlistScheduler
import security.ValidationRules
import sun.security.util.KeyUtil.validate
import java.lang.foreign.Arena.global

fun main() {
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // DI
    configureDI()

    // фоновые задачи (лист ожидания)
    val scheduler = WaitlistScheduler(get(), ConfigProvider.botConfig.waitlist.periodMs)
    scheduler.start()

    // auth
    configureAuth()

    // rate limit
    val rateLimitConf = RateLimitConfig.load()
    install(RateLimit) {
        global {
            rateLimiter(limit = rateLimitConf.requests, refillPeriod = rateLimitConf.window)
        }
    }

    // валидация запросов
    install(RequestValidation) {
        validate<String> { phone -> ValidationRules.validatePhone(phone) }

        validate<GuestDTO> { dto -> ValidationRules.validateGuestName(dto.name) }

        validate<BookingRequest> { br ->
            br.phone?.let { p ->
                when (val res = ValidationRules.validatePhone(p)) {
                    is ValidationResult.Invalid -> return@validate res
                    else -> {}
                }
            }
            br.bookingGuestName?.let { n ->
                when (val res = ValidationRules.validateGuestName(n)) {
                    is ValidationResult.Invalid -> return@validate res
                    else -> {}
                }
            }
            ValidationResult.Valid
        }
    }

    // обработка ошибок
    install(StatusPages) {
        exception<RequestValidationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<RateLimitExceededException> { call, _ ->
            call.respond(HttpStatusCode.TooManyRequests)
        }
    }

    // метрики и роуты
    configureMetrics()
    configureRouting()
}
