package com.bookingbot.api.services

import com.bookingbot.api.model.Club
import com.bookingbot.api.tables.ClubsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class ClubService {

    /**
     * Возвращает список всех клубов из базы данных.
     * @return List of Club objects.
     */
    fun getAllClubs(): List<Club> = transaction {
        ClubsTable.selectAll().map { it.toClub() }
    }

    /**
     * Находит клуб по его уникальному ID.
     * @param id The ID of the club.
     * @return A Club object or null if not found.
     */
    fun findClubById(id: Int): Club? = transaction {
        ClubsTable.select { ClubsTable.id eq id }.map { it.toClub() }.singleOrNull()
    }

    /**
     * Создает новый клуб в базе данных.
     * @param name The name of the new club.
     * @param description An optional description for the new club.
     * @return The newly created Club object.
     */
    fun createClub(name: String, description: String?): Club = transaction {
        val id = ClubsTable.insertAndGetId {
            it[ClubsTable.name] = name
            it[ClubsTable.description] = description
        }
        findClubById(id.value)!!
    }

    /**
     * Устанавливает или обновляет ID административного Telegram-канала для клуба.
     * @param clubId The ID of the club to update.
     * @param channelId The new admin channel ID.
     * @return true if the update was successful, false otherwise.
     */
    fun setAdminChannel(clubId: Int, channelId: Long): Boolean = transaction {
        ClubsTable.update({ ClubsTable.id eq clubId }) {
            it[adminChannelId] = channelId
        } > 0
    }

    /**
     * Включает/отключает список гостей и задаёт максимальное количество гостей.
     */
    fun setGuestList(clubId: Int, enabled: Boolean, maxGuests: Int?): Boolean = transaction {
        ClubsTable.update({ ClubsTable.id eq clubId }) {
            it[hasGuestList] = enabled
            it[maxGuestListSize] = maxGuests
        } > 0
    }

    /**
     * Приватная функция-расширение для конвертации строки из базы данных (ResultRow) в объект Club.
     */
    private fun ResultRow.toClub(): Club = Club(
        id = this[ClubsTable.id].value,
        name = this[ClubsTable.name],
        description = this[ClubsTable.description] ?: "",
        adminChannelId = this[ClubsTable.adminChannelId],
        hasGuestList = this[ClubsTable.hasGuestList],
        maxGuestListSize = this[ClubsTable.maxGuestListSize]
    )
}