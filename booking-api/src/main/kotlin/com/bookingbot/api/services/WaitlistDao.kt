package com.bookingbot.api.services

import com.bookingbot.api.model.waitlist.WaitEntry
import com.bookingbot.api.tables.WaitlistTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/** DAO for waiting_list operations. */
object WaitlistDao {
    fun addEntry(chatId: Long, desiredTime: java.time.Instant, preferredTable: Int?): WaitEntry = transaction {
        val id = WaitlistTable.insertAndGetId {
            it[WaitlistTable.chatId] = chatId
            it[WaitlistTable.desiredTime] = desiredTime
            it[WaitlistTable.preferredTable] = preferredTable
        }.value
        getEntry(id)!!
    }

    fun getEntry(id: Int): WaitEntry? = transaction {
        WaitlistTable.select { WaitlistTable.id eq id }
            .map { it.toWaitEntry() }
            .singleOrNull()
    }

    fun findActive(): List<WaitEntry> = transaction {
        WaitlistTable
            .select { WaitlistTable.status eq "ACTIVE" }
            .map { it.toWaitEntry() }
    }

    fun updateStatus(id: Int, status: String) = transaction {
        WaitlistTable.update({ WaitlistTable.id eq id }) { it[WaitlistTable.status] = status }
    }

    private fun ResultRow.toWaitEntry() = WaitEntry(
        id = this[WaitlistTable.id].value,
        chatId = this[WaitlistTable.chatId],
        desiredTime = this[WaitlistTable.desiredTime],
        preferredTable = this[WaitlistTable.preferredTable],
        status = this[WaitlistTable.status],
        createdAt = this[WaitlistTable.createdAt]
    )
}
