package com.bookingbot.api.model.booking

import java.math.BigDecimal

data class Table(
    val id: Int,
    val clubId: Int,
    val number: Int,
    val capacity: Int,
    val minDeposit: BigDecimal
)
