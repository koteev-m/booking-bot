package com.bookingbot.api.model

enum class UserRole {
    GUEST, PROMOTER, ADMIN, OWNER
}

data class User(
    val telegramId: Long,
    val username: String?,
    val role: UserRole
)