package com.bookingbot.api.services

import com.bookingbot.api.model.User
import com.bookingbot.api.model.UserRole
import com.bookingbot.api.tables.UsersTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class UserService {
    /**
     * Находит пользователя по ID или создает нового, если он не найден.
     */
    fun findOrCreateUser(telegramId: Long, username: String?): User = transaction {
        val existingUser = UsersTable.select { UsersTable.telegramId eq telegramId }.singleOrNull()

        if (existingUser != null) {
            User(
                telegramId = existingUser[UsersTable.telegramId],
                username = existingUser[UsersTable.username],
                role = UserRole.valueOf(existingUser[UsersTable.role])
            )
        } else {
            UsersTable.insertAndGetId {
                it[UsersTable.telegramId] = telegramId
                it[UsersTable.username] = username
                it[UsersTable.role] = UserRole.GUEST.name // По умолчанию все новые пользователи - гости
            }
            User(telegramId = telegramId, username = username, role = UserRole.GUEST)
        }
    }
}
