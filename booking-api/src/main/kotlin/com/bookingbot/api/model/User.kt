package com.bookingbot.api.model

enum class UserRole {
    GUEST, PROMOTER, ADMIN, OWNER, ENTRANCE_MANAGER
}

data class User(
    val telegramId: Long,
    val username: String?,
    val role: UserRole
)