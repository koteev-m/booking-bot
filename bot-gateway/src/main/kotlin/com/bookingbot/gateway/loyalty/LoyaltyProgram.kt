package com.bookingbot.gateway.loyalty

import com.bookingbot.gateway.TelegramApi
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.sql.SQLException
import com.github.kotlintelegrambot.TelegramApiException

/**
 * Loyalty points record.
 */
data class LoyaltyPoint(
    val chatId: Long,
    val currentPoints: Int,
    val lifetimePoints: Int
)

/** Reward types available for redemption. */
enum class RewardType { DISCOUNT, COMPLIMENT, MERCH }

/**
 * Loyalty reward data.
 */
data class LoyaltyReward(
    val id: Int,
    val title: String,
    val costPoints: Int,
    val rewardType: RewardType
)

/** Repository interface for loyalty operations. */
interface LoyaltyRepository {
    /** Increase guest balance for a visit. */
    fun addVisit(chatId: Long)

    /** Get current points balance. */
    fun getBalance(chatId: Long): Int

    /** List available rewards. */
    fun listRewards(): List<LoyaltyReward>

    /** Redeem a reward if balance is sufficient. */
    fun redeem(chatId: Long, rewardId: Int): Boolean
}

object LoyaltyPointsTable : Table("loyalty_points") {
    val chatId = long("chat_id")
    val currentPoints = integer("current_points").default(0)
    val lifetimePoints = integer("lifetime_points").default(0)
    override val primaryKey = PrimaryKey(chatId)
}

object LoyaltyRewardsTable : IntIdTable("loyalty_rewards") {
    val title = text("title")
    val costPoints = integer("cost_points")
    val rewardType = varchar("reward_type", 20)
}

object LoyaltyRedemptionsTable : IntIdTable("loyalty_redemptions") {
    val chatId = long("chat_id").references(LoyaltyPointsTable.chatId)
    val rewardId = reference("reward_id", LoyaltyRewardsTable)
    val createdAt = datetime("created_at").clientDefault { java.time.LocalDateTime.now() }
}

class LoyaltyRepositoryImpl(
    private val pointsPerVisit: Int
) : LoyaltyRepository {
    private val logger = LoggerFactory.getLogger(LoyaltyRepositoryImpl::class.java)

    override fun addVisit(chatId: Long) {
        try {
            transaction {
                val existing = LoyaltyPointsTable.select { LoyaltyPointsTable.chatId eq chatId }.singleOrNull()
                if (existing == null) {
                    LoyaltyPointsTable.insert {
                        it[LoyaltyPointsTable.chatId] = chatId
                        it[currentPoints] = pointsPerVisit
                        it[lifetimePoints] = pointsPerVisit
                    }
                } else {
                    LoyaltyPointsTable.update({ LoyaltyPointsTable.chatId eq chatId }) {
                        with(SqlExpressionBuilder) {
                            it.update(currentPoints, currentPoints + pointsPerVisit)
                            it.update(lifetimePoints, lifetimePoints + pointsPerVisit)
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error("Failed to add loyalty visit", e)
        }
    }

    override fun getBalance(chatId: Long): Int = try {
        transaction {
            LoyaltyPointsTable.select { LoyaltyPointsTable.chatId eq chatId }
                .map { it[LoyaltyPointsTable.currentPoints] }
                .singleOrNull() ?: 0
        }
    } catch (e: SQLException) {
        logger.error("Failed to get loyalty balance", e)
        0
    }

    override fun listRewards(): List<LoyaltyReward> = try {
        transaction {
            LoyaltyRewardsTable.selectAll().map {
                LoyaltyReward(
                    id = it[LoyaltyRewardsTable.id].value,
                    title = it[LoyaltyRewardsTable.title],
                    costPoints = it[LoyaltyRewardsTable.costPoints],
                    rewardType = RewardType.valueOf(it[LoyaltyRewardsTable.rewardType])
                )
            }
        }
    } catch (e: SQLException) {
        logger.error("Failed to list rewards", e)
        emptyList()
    }

    override fun redeem(chatId: Long, rewardId: Int): Boolean = try {
        transaction {
            val reward = LoyaltyRewardsTable.select { LoyaltyRewardsTable.id eq rewardId }
                .singleOrNull() ?: return@transaction false
            val cost = reward[LoyaltyRewardsTable.costPoints]
            val current = LoyaltyPointsTable.select { LoyaltyPointsTable.chatId eq chatId }
                .singleOrNull()?.get(LoyaltyPointsTable.currentPoints) ?: 0
            if (current < cost) return@transaction false

            LoyaltyPointsTable.update({ LoyaltyPointsTable.chatId eq chatId }) {
                with(SqlExpressionBuilder) { it.update(currentPoints, currentPoints - cost) }
            }
            LoyaltyRedemptionsTable.insert {
                it[LoyaltyRedemptionsTable.chatId] = chatId
                it[LoyaltyRedemptionsTable.rewardId] = rewardId
            }
            true
        }
    } catch (e: SQLException) {
        logger.error("Failed to redeem reward", e)
        false
    }
}

/** Register loyalty related Telegram handlers. */
fun addLoyaltyHandlers(dispatcher: Dispatcher, repo: LoyaltyRepository) {
    val logger = LoggerFactory.getLogger("LoyaltyHandlers")

    dispatcher.command("visit") {
        val chatId = message.chat.id
        try {
            repo.addVisit(chatId)
            val balance = repo.getBalance(chatId)
            TelegramApi.sendMessage(ChatId.fromId(chatId), "Спасибо за визит! Ваш баланс: $balance")
        } catch (e: TelegramApiException) {
            logger.error("Telegram error on /visit", e)
        }
    }

    dispatcher.command("balance") {
        val chatId = message.chat.id
        try {
            val balance = repo.getBalance(chatId)
            TelegramApi.sendMessage(ChatId.fromId(chatId), "Ваш баланс: $balance")
        } catch (e: TelegramApiException) {
            logger.error("Telegram error on /balance", e)
        }
    }

    dispatcher.command("rewards") {
        val chatId = message.chat.id
        try {
            val rewards = repo.listRewards()
            val buttons = rewards.map { reward ->
                listOf(
                    InlineKeyboardButton.CallbackData(
                        text = "${reward.title} (${reward.costPoints})",
                        callbackData = "redeem_${reward.id}"
                    )
                )
            }
            TelegramApi.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Доступные награды:",
                replyMarkup = InlineKeyboardMarkup.create(buttons)
            )
        } catch (e: TelegramApiException) {
            logger.error("Telegram error on /rewards", e)
        }
    }

    dispatcher.callbackQuery { callbackQuery ->
        val data = callbackQuery.data
        if (!data.startsWith("redeem_")) return@callbackQuery
        val rewardId = data.substringAfter("redeem_").toIntOrNull() ?: return@callbackQuery
        val chatId = callbackQuery.from.id
        try {
            val success = repo.redeem(chatId, rewardId)
            val text = if (success) "Награда успешно получена!" else "Недостаточно баллов." 
            TelegramApi.sendMessage(ChatId.fromId(chatId), text)
            bot.answerCallbackQuery(callbackQuery.id)
        } catch (e: TelegramApiException) {
            logger.error("Telegram error on redeem", e)
        }
    }
}

