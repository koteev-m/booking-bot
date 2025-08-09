package com.bookingbot.api.services

import com.bookingbot.api.model.GuestListEntry
import com.bookingbot.api.tables.GuestListTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction

class GuestListService(private val clubService: ClubService) {
    fun addGuest(promoterId: Long, clubId: Int, firstName: String, lastName: String): Boolean = transaction {
        val club = clubService.findClubById(clubId) ?: return@transaction false
        if (!club.hasGuestList) return@transaction false
        val current = GuestListTable.select { GuestListTable.clubId eq clubId }.count()
        if (club.maxGuestListSize != null && current >= club.maxGuestListSize) return@transaction false
        GuestListTable.insert {
            it[GuestListTable.clubId] = clubId
            it[GuestListTable.promoterId] = promoterId
            it[GuestListTable.firstName] = firstName
            it[GuestListTable.lastName] = lastName
        }
        true
    }

    fun getGuestsForClub(clubId: Int): List<GuestListEntry> = transaction {
        GuestListTable.select { GuestListTable.clubId eq clubId }.map {
            GuestListEntry(
                id = it[GuestListTable.id].value,
                clubId = it[GuestListTable.clubId],
                promoterId = it[GuestListTable.promoterId],
                firstName = it[GuestListTable.firstName],
                lastName = it[GuestListTable.lastName],
                checkedIn = it[GuestListTable.checkedIn]
            )
        }
    }

    fun checkInGuest(guestId: Int): Boolean = transaction {
        GuestListTable.update({ GuestListTable.id eq guestId }) {
            it[checkedIn] = true
        } > 0
    }
}
