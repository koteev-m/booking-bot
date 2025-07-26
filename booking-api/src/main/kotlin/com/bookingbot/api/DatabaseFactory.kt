package com.bookingbot.api

import com.bookingbot.api.tables.BookingsTable
import com.bookingbot.api.tables.WaitlistTable
import com.bookingbot.api.PromoterStatsTable
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

        val password = System.getenv("DB_PASS")?.takeIf(String::isNotBlank)
            ?: if (config.hasPath("db.pass")) config.getString("db.pass") else ""

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
                SchemaUtils.create(BookingsTable, WaitlistTable, PromoterStatsTable)
            }
        }
    }

    /**
     * Проверяет существование таблицы в текущей базе данных.
     * Ранее название таблицы просто конкатенировалось в SQL-запрос, что
     * позволяло выполнить SQL‑инъекцию. Теперь имя таблицы проверяется по
     * небольшому списку разрешённых значений перед выполнением запроса.
     *
     * @throws IllegalArgumentException если tableName не входит в whitelist.
     */
    fun exists(tableName: String): Boolean = transaction {
        val allowed = setOf("bookings", "tables", "promoters")
        require(tableName in allowed) { "Unknown table name" }
        try {
            exec("SELECT 1 FROM $tableName LIMIT 1") { rs -> rs.next() } ?: false
        } catch (e: Exception) {
            false
        }
    }
}