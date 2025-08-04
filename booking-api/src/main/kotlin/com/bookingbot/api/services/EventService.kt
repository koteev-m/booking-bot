package com.bookingbot.api.services

import com.bookingbot.api.model.Event
import com.bookingbot.api.tables.EventsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class EventService {
    /**
     * Создает новое событие/афишу для клуба.
     */
    fun createEvent(
        clubId: Int,
        title: String,
        description: String?,
        eventDate: Instant,
        imageUrl: String?
    ): Event = transaction {
        val id = EventsTable.insertAndGetId {
            it[EventsTable.clubId] = clubId
            it[EventsTable.title] = title
            it[EventsTable.description] = description
            it[EventsTable.eventDate] = eventDate
            it[EventsTable.imageUrl] = imageUrl
            it[EventsTable.createdAt] = Instant.now()
        }
        findEventById(id.value)!!
    }

    /**
     * Находит предстоящие события для клуба.
     */
    fun findUpcomingEventsByClub(clubId: Int): List<Event> = transaction {
        EventsTable
            .select { (EventsTable.clubId eq clubId) and (EventsTable.eventDate greaterEq Instant.now()) }
            .orderBy(EventsTable.eventDate, SortOrder.ASC)
            .map { it.toEvent() }
    }

    private fun findEventById(id: Int): Event? = transaction {
        EventsTable.select { EventsTable.id eq id }.map { it.toEvent() }.singleOrNull()
    }

    private fun ResultRow.toEvent(): Event = Event(
        id = this[EventsTable.id].value,
        clubId = this[EventsTable.clubId],
        title = this[EventsTable.title],
        description = this[EventsTable.description],
        eventDate = this[EventsTable.eventDate],
        imageUrl = this[EventsTable.imageUrl]
    )
}

