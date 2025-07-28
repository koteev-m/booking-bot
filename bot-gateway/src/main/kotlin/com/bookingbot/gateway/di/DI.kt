package com.bookingbot.gateway.di

import com.bookingbot.gateway.di.BookingService
import com.bookingbot.gateway.hall.HallSchemeRenderer
import org.koin.dsl.module

/** Gateway Koin module. */
val gatewayModule = module {
    single { HallSchemeRenderer(get()) }
    factory<BookingService> { BookingServiceImpl(get(), get()) }
}
