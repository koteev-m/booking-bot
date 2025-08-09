package com.bookingbot.api

import com.bookingbot.api.tables.BookingsTable
import com.bookingbot.api.tables.ClubStaffTable
import com.bookingbot.api.tables.ClubsTable
import com.bookingbot.api.tables.EventsTable
import com.bookingbot.api.tables.PromoterStatsTable
import com.bookingbot.api.tables.TablesTable
import com.bookingbot.api.tables.UsersTable
import com.bookingbot.api.tables.WaitlistTable
import com.bookingbot.api.tables.GuestListTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    private lateinit var dataSource: HikariDataSource

    /**
     * Инициализация подключения и (для Postgres) миграций Flyway.
     * Сначала пытаемся взять из переменных окружения,
     * затем — из application.conf (HOCON), и в крайнем случае — H2 in-memory.
     */
    fun init() {
        val config = ConfigFactory.load()

        val url = System.getenv("DB_URL")?.takeIf(String::isNotBlank)
            ?: (if (config.hasPath("db.url")) config.getString("db.url").takeIf(String::isNotBlank) else null)
            ?: "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"

        val user = System.getenv("DB_USER")?.takeIf(String::isNotBlank)
            ?: if (config.hasPath("db.user")) config.getString("db.user") else "sa"

        val password = System.getenv("DB_PASSWORD")?.takeIf(String::isNotBlank)
            ?: if (config.hasPath("db.password")) config.getString("db.password") else ""

        val driver = when {
            url.startsWith("jdbc:h2")         -> "org.h2.Driver"
            url.startsWith("jdbc:postgresql") -> "org.postgresql.Driver"
            else                              -> throw IllegalArgumentException("Unknown JDBC URL: $url")
        }

        val configHikari = HikariConfig().apply {
            jdbcUrl = url
            this.username = user
            this.password = password
            this.driverClassName = driver
        }
        dataSource = HikariDataSource(configHikari)

        // Миграции Flyway для реальной БД
        if (!url.startsWith("jdbc:h2")) {
            Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate()
        }

        // Подключение к Exposed
        Database.connect(dataSource)

        // Для H2 in-memory создаём схему через Exposed
        if (url.startsWith("jdbc:h2")) {
            transaction {
                SchemaUtils.create(
                    UsersTable,
                    ClubsTable,
                    ClubStaffTable,
                    TablesTable,
                    BookingsTable,
                    EventsTable,
                    WaitlistTable,
                    PromoterStatsTable,
                    GuestListTable
                )
            }
        }
    }

    /**
     * Checks table existence without risking SQL injection.
     * Accepts only known tables defined in [allowedTables].
     *
     * @throws IllegalArgumentException if tableName is not allowed.
     */
    fun exists(tableName: String): Boolean {
        val allowedTables = setOf(
            "bookings",
            "tables",
            "promoters",
            "waiting_list",
            "loyalty_points",
            "guest_list"
        )
        require(tableName in allowedTables) { "Unknown table: $tableName" }
        return transaction {
            exec("SELECT 1 FROM $tableName LIMIT 1") { rs -> rs.next() } ?: false
        }
    }
}