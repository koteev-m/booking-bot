package com.bookingbot.api.services

import com.bookingbot.api.model.Club
import com.bookingbot.api.tables.ClubsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ClubService {
    /**
     * Возвращает список всех клубов из базы данных.
     */
    fun getAllClubs(): List<Club> = transaction {
        ClubsTable.selectAll().map { it.toClub() }
    }

    /**
     * Находит клуб по его ID.
     */
    fun findClubById(id: Int): Club? = transaction {
        ClubsTable.select { ClubsTable.id eq id }.map { it.toClub() }.singleOrNull()
    }

    private fun ResultRow.toClub(): Club = Club(
        id = this[ClubsTable.id].value,
        name = this[ClubsTable.name],
        description = this[ClubsTable.description] ?: "",
        adminChannelId = this[ClubsTable.adminChannelId]
    )
}