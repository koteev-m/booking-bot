package com.bookingbot.api.model.booking

// Модель для представления стола
data class Table(
    val id: Int,
    val number: Int,
    val capacity: Int
)