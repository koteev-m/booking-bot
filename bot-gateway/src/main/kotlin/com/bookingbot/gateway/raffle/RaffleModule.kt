package com.bookingbot.gateway.raffle

import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.UserService
import com.bookingbot.api.model.UserRole
import com.bookingbot.gateway.Bot
import com.bookingbot.gateway.TelegramApi
import com.github.kotlintelegrambot.TelegramApiException
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.payments.LabeledPrice
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

/** Data about raffle with closing time. */
data class Raffle(val id: Int, val closesAt: Instant, val finished: Boolean)

/** Participant of raffle with weighting. */
data class Participant(val raffleId: Int, val chatId: Long, val weight: Int)

/** Prize sealed hierarchy. */
sealed interface Prize {
    data class Premium(val months: Int) : Prize
    data class Stars(val amount: Int) : Prize
    data class ClubGift(val description: String) : Prize
}

object RafflesTable : IntIdTable("raffles") {
    val closesAt = timestamp("closes_at")
    val finished = bool("finished").default(false)
}

object ParticipantsTable : Table("participants") {
    val raffleId = integer("raffle_id").references(RafflesTable.id)
    val chatId = long("chat_id")
    val weight = integer("weight")
    override val primaryKey = PrimaryKey(raffleId, chatId)
}

object PrizesTable : IntIdTable("prizes") {
    val raffleId = integer("raffle_id").references(RafflesTable.id)
    val type = varchar("type", 20)
    val data = text("data")
}

/** Service managing raffles and winner selection. */
object RaffleService {
    private val logger = LoggerFactory.getLogger(RaffleService::class.java)
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    init {
        scheduler.scheduleAtFixedRate({ pickWinners() }, 0, 1, TimeUnit.MINUTES)
    }

    fun createRaffle(prizes: List<Prize>, closesAt: Instant): Int = transaction {
        val id = RafflesTable.insertAndGetId { it[this.closesAt] = closesAt }.value
        prizes.forEach { prize ->
            PrizesTable.insert {
                it[raffleId] = id
                when (prize) {
                    is Prize.Premium -> {
                        it[type] = "PREMIUM"
                        it[data] = prize.months.toString()
                    }
                    is Prize.Stars -> {
                        it[type] = "STARS"
                        it[data] = prize.amount.toString()
                    }
                    is Prize.ClubGift -> {
                        it[type] = "CLUB"
                        it[data] = prize.description
                    }
                }
            }
        }
        id
    }

    fun joinRaffle(raffleId: Int, chatId: Long, hasBooking: Boolean) = transaction {
        val weight = if (hasBooking) 3 else 1
        val exists = ParticipantsTable.select { (ParticipantsTable.raffleId eq raffleId) and (ParticipantsTable.chatId eq chatId) }.singleOrNull()
        if (exists == null) {
            ParticipantsTable.insert {
                it[this.raffleId] = raffleId
                it[this.chatId] = chatId
                it[this.weight] = weight
            }
        } else {
            ParticipantsTable.update({ (ParticipantsTable.raffleId eq raffleId) and (ParticipantsTable.chatId eq chatId) }) {
                it[this.weight] = weight
            }
        }
    }

    private fun getDueRaffles(now: Instant): List<Raffle> = transaction {
        RafflesTable
            .select { (RafflesTable.closesAt lessEq now) and RafflesTable.finished.eq(false) }
            .map { it.toRaffle() }
    }

    fun pickWinners() {
        val raffles = try {
            getDueRaffles(Instant.now())
        } catch (e: SQLException) {
            logger.error("SQL error when loading raffles", e)
            return
        }

        raffles.forEach { raffle ->
            try {
                val participants = transaction {
                    ParticipantsTable.select { ParticipantsTable.raffleId eq raffle.id }
                        .map { it.toParticipant() }
                }
                val prizes = transaction {
                    PrizesTable.select { PrizesTable.raffleId eq raffle.id }
                        .map { it.toPrize() }
                }

                val mutableParts = participants.toMutableList()
                val rnd = ThreadLocalRandom.current()

                prizes.forEach { prize ->
                    if (mutableParts.isEmpty()) return@forEach
                    val total = mutableParts.sumOf { it.weight }
                    var roll = rnd.nextInt(total)
                    var winner: Participant? = null
                    for (p in mutableParts) {
                        roll -= p.weight
                        if (roll < 0) { winner = p; break }
                    }
                    winner?.let {
                        PrizeDispatcher.dispatch(it.chatId, prize)
                        mutableParts.remove(it)
                    }
                }

                transaction { RafflesTable.update({ RafflesTable.id eq raffle.id }) { it[finished] = true } }
            } catch (e: SQLException) {
                logger.error("SQL error when picking winners", e)
            } catch (e: TelegramApiException) {
                logger.error("Telegram error when notifying winners", e)
            }
        }
    }

    fun getActiveRaffle(): Raffle? = transaction {
        RafflesTable
            .select { RafflesTable.finished eq false }
            .orderBy(RafflesTable.id, SortOrder.DESC)
            .limit(1)
            .map { it.toRaffle() }
            .firstOrNull()
    }

    private fun ResultRow.toRaffle() = Raffle(
        id = this[RafflesTable.id].value,
        closesAt = this[RafflesTable.closesAt],
        finished = this[RafflesTable.finished]
    )

    private fun ResultRow.toParticipant() = Participant(
        raffleId = this[ParticipantsTable.raffleId],
        chatId = this[ParticipantsTable.chatId],
        weight = this[ParticipantsTable.weight]
    )

    private fun ResultRow.toPrize(): Prize = when (this[PrizesTable.type]) {
        "PREMIUM" -> Prize.Premium(this[PrizesTable.data].toIntOrNull() ?: 1)
        "STARS" -> Prize.Stars(this[PrizesTable.data].toIntOrNull() ?: 0)
        else -> Prize.ClubGift(this[PrizesTable.data])
    }
}

object PrizeDispatcher {
    private val logger = LoggerFactory.getLogger(PrizeDispatcher::class.java)

    fun dispatch(chatId: Long, prize: Prize) {
        try {
            when (prize) {
                is Prize.Premium -> Bot.instance.giftPremiumSubscription(ChatId.fromId(chatId))
                is Prize.Stars -> Bot.instance.sendInvoice(
                    chatId = ChatId.fromId(chatId),
                    title = "Telegram Stars",
                    description = "Подарок: ${prize.amount} Stars",
                    payload = "stars_${System.currentTimeMillis()}",
                    providerToken = "",
                    currency = "XTR",
                    prices = listOf(LabeledPrice("Stars", prize.amount))
                )
                is Prize.ClubGift -> TelegramApi.sendMessage(ChatId.fromId(chatId), prize.description)
            }
        } catch (e: TelegramApiException) {
            logger.error("Failed to dispatch prize", e)
        }
    }
}

/** Register raffle related handlers. */
fun addRaffleHandlers(dispatcher: Dispatcher, bookingService: BookingService, userService: UserService) {
    dispatcher.command("raffle_create") {
        val adminId = message.from?.id ?: return@command
        val admin = userService.findOrCreateUser(adminId, message.from?.username)
        if (admin.role != UserRole.ADMIN && admin.role != UserRole.OWNER) {
            TelegramApi.sendMessage(ChatId.fromId(adminId), "У вас нет прав для этой команды.")
            return@command
        }

        val minutes = args.firstOrNull()?.toLongOrNull() ?: 10L
        val closesAt = Instant.now().plusSeconds(minutes * 60)
        val defaultPrizes = listOf(
            Prize.Premium(1),
            Prize.Stars(100),
            Prize.ClubGift("Ваучер на депозит")
        )
        val id = RaffleService.createRaffle(defaultPrizes, closesAt)
        TelegramApi.sendMessage(ChatId.fromId(adminId), "Розыгрыш #$id создан и завершится через $minutes минут.")
    }

    dispatcher.command("raffle_announce") {
        val adminId = message.from?.id ?: return@command
        val admin = userService.findOrCreateUser(adminId, message.from?.username)
        if (admin.role != UserRole.ADMIN && admin.role != UserRole.OWNER) {
            TelegramApi.sendMessage(ChatId.fromId(adminId), "У вас нет прав для этой команды.")
            return@command
        }
        val raffle = RaffleService.getActiveRaffle()
        if (raffle == null) {
            TelegramApi.sendMessage(ChatId.fromId(adminId), "Нет активных розыгрышей")
            return@command
        }
        val dt = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault()).format(raffle.closesAt)
        val keyboard = InlineKeyboardMarkup.create(listOf(listOf(
            InlineKeyboardButton.CallbackData("\uD83C\uDF81 Участвовать", "JOIN_${raffle.id}")
        )))
        TelegramApi.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "\uD83C\uDF81 **Большой розыгрыш!** До $dt. Нажмите кнопку ниже, чтобы присоединиться.",
            parseMode = ParseMode.MARKDOWN,
            replyMarkup = keyboard
        )
    }

    dispatcher.callbackQuery {
        val data = callbackQuery.data ?: return@callbackQuery
        if (!data.startsWith("JOIN_")) return@callbackQuery
        val raffleId = data.removePrefix("JOIN_").toIntOrNull() ?: return@callbackQuery
        val userId = callbackQuery.from.id
        val hasBooking = try {
            bookingService.findBookingsByUserId(userId)
                .any { it.status in listOf("PENDING", "SEATED", "CONFIRMED") }
        } catch (e: Exception) { false }
        RaffleService.joinRaffle(raffleId, userId, hasBooking)
        bot.answerCallbackQuery(callbackQuery.id, text = "Вы участвуете в розыгрыше!")
    }
}
