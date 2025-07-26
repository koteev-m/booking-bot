// Gradle dependencies:
// implementation("io.github.kotlin-telegram-bot:kotlin-telegram-bot:6.1.0")
// implementation("org.jetbrains.exposed:exposed-core:0.49.0")
// implementation("org.jetbrains.exposed:exposed-dao:0.49.0")
// implementation("org.jetbrains.exposed:exposed-jdbc:0.49.0")

package admin

import com.bookingbot.api.tables.BookingsTable
import com.bookingbot.api.tables.TablesTable
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.TelegramApiException
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import java.io.File
import java.math.BigDecimal
import java.sql.SQLException
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

private const val STATS_DOW = "STATS_DOW"
private const val STATS_TOPTABLES = "STATS_TOPTABLES"
private const val STATS_FINANCE = "STATS_FINANCE"

private const val ADMIN_IDS_SECRET_PATH = "/run/secrets/admin_ids"

/** Set of Telegram IDs allowed to view dashboard. */
val ADMIN_IDS: Set<Long> = run {
    val file = File(ADMIN_IDS_SECRET_PATH)
    val ids = if (file.exists()) file.readText().trim() else System.getenv("ADMIN_IDS")
    ids?.split(',')
        ?.mapNotNull { it.trim().toLongOrNull() }
        ?.toSet()
        ?: emptySet()
}

/** Returns true if [id] belongs to an admin. */
fun isAdmin(id: Long?): Boolean = id != null && id in ADMIN_IDS

private val logger = LoggerFactory.getLogger("AdminDashboard")

/**
 * Service calculating admin statistics using Exposed DSL.
 */
object StatsService {
    /** Load statistics grouped by weekday (Mon..Sun). */
    fun getLoadByWeekday(): List<Pair<String, Int>> = transaction {
        BookingsTable
            .slice(BookingsTable.bookingTime)
            .selectAll()
            .map { it[BookingsTable.bookingTime] }
            .groupingBy { it.atZone(ZoneId.systemDefault()).dayOfWeek }
            .eachCount()
    }.let { counts ->
        DayOfWeek.values().map { dow ->
            dow.name.substring(0, 3) to (counts[dow] ?: 0)
        }
    }

    /** Top booked tables sorted by popularity. */
    fun getTopTables(limit: Int = 5): List<Pair<Int, Int>> = transaction {
        val cnt = BookingsTable.id.count()
        BookingsTable
            .slice(BookingsTable.tableId, cnt)
            .selectAll()
            .groupBy(BookingsTable.tableId)
            .orderBy(cnt, SortOrder.DESC)
            .limit(limit)
            .map { it[BookingsTable.tableId] to it[cnt] }
    }

    /** Total deposit value for bookings in the last [days] days. */
    fun getFinanceTotal(days: Int = 30): BigDecimal = transaction {
        val since = Instant.now().minus(days.toLong(), ChronoUnit.DAYS)
        (BookingsTable innerJoin TablesTable)
            .slice(TablesTable.minDeposit, BookingsTable.partySize)
            .select { BookingsTable.bookingTime greaterEq since }
            .map { row ->
                row[TablesTable.minDeposit].multiply(BigDecimal(row[BookingsTable.partySize]))
            }
            .fold(BigDecimal.ZERO, BigDecimal::add)
    }
}

/**
 * Send admin dashboard with statistic buttons.
 */
fun Bot.sendAdminDashboard(chatId: Long) {
    val keyboard = InlineKeyboardMarkup.create(
        listOf(
            listOf(InlineKeyboardButton.CallbackData("\uD83D\uDCC8 Weekdays", STATS_DOW)),
            listOf(InlineKeyboardButton.CallbackData("\uD83D\uDD25 Top Tables", STATS_TOPTABLES)),
            listOf(InlineKeyboardButton.CallbackData("\uD83D\uDCB5 Deposits", STATS_FINANCE))
        )
    )
    try {
        sendMessage(
            chatId = ChatId.fromId(chatId),
            text = "\uD83D\uDCCA Admin Dashboard",
            parseMode = ParseMode.MARKDOWN,
            replyMarkup = keyboard
        )
    } catch (e: TelegramApiException) {
        logger.error("Failed to send admin dashboard", e)
    }
}

/**
 * Handle dashboard button callbacks and display metrics.
 */
context(CallbackQueryHandlerEnvironment)
fun Bot.handleDashboardCallbacks() {
    val data = callbackQuery.data ?: return
    val message = callbackQuery.message ?: return
    val chatId = message.chat.id
    val msgId = message.messageId
    try {
        val text = when (data) {
            STATS_DOW -> {
                val rows = StatsService.getLoadByWeekday()
                buildString {
                    append("*Load by Weekday*\n")
                    append("Day | Count\n")
                    append("---|---\n")
                    rows.forEach { (d, c) -> append("$d | $c\n") }
                }
            }

            STATS_TOPTABLES -> {
                val rows = StatsService.getTopTables()
                buildString {
                    append("*Top Tables*\n")
                    append("Table | Count\n")
                    append("---|---\n")
                    rows.forEach { (id, c) -> append("$id | $c\n") }
                }
            }

            STATS_FINANCE -> {
                val sum = StatsService.getFinanceTotal()
                "*Total deposits last 30 days:* $sum"
            }

            else -> return
        }
        editMessageText(
            chatId = ChatId.fromId(chatId),
            messageId = msgId,
            text = text,
            parseMode = ParseMode.MARKDOWN
        )
        answerCallbackQuery(callbackQuery.id)
    } catch (e: SQLException) {
        logger.error("SQL error in dashboard callback", e)
    } catch (e: TelegramApiException) {
        logger.error("Telegram error in dashboard callback", e)
    }
}

/*
Example dispatcher registration:

dispatch {
    command("dashboard") { if (isAdmin(message.from?.id)) bot.sendAdminDashboard(chatId) }
    callbackQuery { bot.handleDashboardCallbacks() }
}
*/
