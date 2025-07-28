package com.bookingbot.gateway.integration

import com.bookingbot.api.DatabaseFactory
import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.WaitlistDao
import com.bookingbot.gateway.waitlist.WaitlistNotifierImpl
import io.ktor.server.testing.testApplication
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class WaitlistIntegrationTest {
    @Test
    fun `cancel booking updates waitlist`() = testApplication {
        val pg = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
        pg.start()
        environment {
            systemProperties["DB_URL"] = pg.jdbcUrl
            systemProperties["DB_USER"] = pg.username
            systemProperties["DB_PASSWORD"] = pg.password
            systemProperties["JWT_SECRET"] = "secret"
            systemProperties["BASIC_USER"] = "a"
            systemProperties["BASIC_PASS"] = "b"
        }
        DatabaseFactory.init()
        val service = BookingService()
        val notifier = WaitlistNotifierImpl(service, com.bookingbot.api.services.TableService())
        com.bookingbot.api.services.WaitlistNotifierHolder.notifier = notifier
        val entry = WaitlistDao.addEntry(1, Instant.EPOCH, 1)
        val booking = service.createBooking(
            BookingRequest(1,1,1,Instant.EPOCH,1,60,"g",1,"+1",null,"test")
        )
        service.cancelBooking(booking.id, 1)
        notifier.scanWaitlist()
        val updated = WaitlistDao.getEntry(entry.id)!!
        assertEquals("OFFERED", updated.status)
        pg.stop()
    }
}
