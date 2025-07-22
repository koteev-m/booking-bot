package com.bookingbot.api.services

import com.bookingbot.api.model.User
import com.bookingbot.api.model.UserRole
import com.bookingbot.api.tables.ClubStaffTable
import com.bookingbot.api.tables.UsersTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

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
            UsersTable.insert {
                it[UsersTable.telegramId] = telegramId
                it[UsersTable.username] = username
                it[UsersTable.role] = UserRole.GUEST.name // По умолчанию все новые пользователи - гости
            }
            User(telegramId = telegramId, username = username, role = UserRole.GUEST)
        }
    }

    /**
     * Обновляет роль пользователя.
     * @return true, если пользователь найден и роль обновлена, иначе false.
     */
    fun updateUserRole(telegramId: Long, newRole: UserRole): Boolean = transaction {
        UsersTable.update({ UsersTable.telegramId eq telegramId }) {
            it[role] = newRole.name
        } > 0
    }

    /**
     * "Привязывает" пользователя к определенному клубу как сотрудника.
     * Сначала обновляет его общую роль, затем создает запись в club_staff.
     * @return true, если операция успешна.
     */
    fun assignUserToClub(userId: Long, clubId: Int, role: UserRole): Boolean {
        // Убеждаемся, что роль соответствует персоналу
        if (role != UserRole.ADMIN && role != UserRole.PROMOTER) {
            return false
        }

        transaction {
            // 1. Обновляем основную роль пользователя
            updateUserRole(userId, role)

            // 2. Добавляем запись в таблицу персонала
            ClubStaffTable.insert {
                it[ClubStaffTable.userId] = userId
                it[ClubStaffTable.clubId] = clubId
            }
        }
        return true
    }

    /**
     * Находит ID клуба, к которому привязан сотрудник (админ/промоутер).
     */
    fun getStaffClubId(staffId: Long): Int? = transaction {
        ClubStaffTable
            .select { ClubStaffTable.userId eq staffId }
            .map { it[ClubStaffTable.clubId] }
            .singleOrNull()
    }
}


