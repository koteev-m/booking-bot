package com.bookingbot.gateway

import com.bookingbot.api.DatabaseFactory
import com.bookingbot.api.services.BookingService
import io.ktor.server.application.Application
import com.bookingbot.gateway.fsm.RedisStateStorage
import com.bookingbot.gateway.fsm.StateStorage
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.core.qualifier.named
import org.koin.core.module.dsl.singleOf

/**
 * Koin module providing application dependencies.
 */
val appModule = module {
    single { DatabaseFactory }
    single { BookingService() }
    single<StateStorage> {
        RedisStateStorage(
            redisUrl = environment.config.property("redis.url").getString()
        )
    }
}

/**
 * Install Koin and register [appModule].
 */
fun Application.configureDI() = install(Koin) {
    slf4jLogger()
    modules(appModule)
}

/*
Gradle dependencies for Koin:
implementation("io.insert-koin:koin-ktor:3.6.0")
implementation("io.insert-koin:koin-logger-slf4j:3.6.0")
testImplementation("io.insert-koin:koin-test:3.6.0")
*/

