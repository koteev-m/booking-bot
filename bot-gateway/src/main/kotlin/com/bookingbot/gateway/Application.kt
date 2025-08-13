package com.bookingbot.gateway

import GuestDTO
import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.gateway.waitlist.WaitlistScheduler
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application as KtorApplication
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.rateLimiter
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.plugins.requestvalidation.validate as validateRequest
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.plugins.statuspages.status
import io.ktor.server.response.respond
import org.koin.ktor.ext.get
import security.ValidationRules

fun main() {
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0"
    ) { module() }.start(wait = true)
}

fun KtorApplication.module() {
    // DI
    configureDI()

    // фоновые задачи (лист ожидания)
    val scheduler = WaitlistScheduler(get(), ConfigProvider.botConfig.waitlist.periodMs)
    scheduler.start()

    // auth
    configureAuth()

    // Rate limit
    val rateLimitConf = RateLimitConfig.load()
    install(RateLimit) {
        global {
            rateLimiter(
                limit = rateLimitConf.requests,
                refillPeriod = rateLimitConf.window
            )
        }
    }

    // Валидация входящих запросов
    install(RequestValidation) {
        validateRequest<String> { phone -> ValidationRules.validatePhone(phone) }
        validateRequest<GuestDTO> { dto -> ValidationRules.validateGuestName(dto.name) }
        validateRequest<BookingRequest> { br ->
            val phoneValidation = br.phone?.let(ValidationRules::validatePhone) ?: ValidationResult.Valid
            if (phoneValidation is ValidationResult.Invalid) return@validateRequest phoneValidation

            val guestNameValidation = br.bookingGuestName?.let(ValidationRules::validateGuestName) ?: ValidationResult.Valid
            if (guestNameValidation is ValidationResult.Invalid) return@validateRequest guestNameValidation

            ValidationResult.Valid
        }
    }

    // Ошибки/статусы
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.reasons.joinToString())
        }
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(status)
        }
    }

    // Метрики и роутинг
    configureMetrics()
    configureRouting()
}
