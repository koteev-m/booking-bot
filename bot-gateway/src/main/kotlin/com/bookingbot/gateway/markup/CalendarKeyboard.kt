package com.bookingbot.gateway.markup

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.bookingbot.gateway.util.CallbackData
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

object CalendarKeyboard {

    private val locale = Locale("ru")

    fun create(year: Int, month: Int): InlineKeyboardMarkup {
        val yearMonth = YearMonth.of(year, month)
        val firstDayOfMonth = yearMonth.atDay(1)
        val daysInMonth = yearMonth.lengthOfMonth()

        val keyboard = mutableListOf<List<InlineKeyboardButton>>()

        // 1. Название месяца и кнопки навигации
        val monthName = yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        keyboard.add(
            listOf(
                InlineKeyboardButton.CallbackData("◀", "${CallbackData.CALENDAR_PREV_PREFIX}${yearMonth}"),
                InlineKeyboardButton.CallbackData("$monthName $year", CallbackData.CALENDAR_IGNORE),
                InlineKeyboardButton.CallbackData("▶", "${CallbackData.CALENDAR_NEXT_PREFIX}${yearMonth}")
            )
        )

        // 2. Дни недели
        keyboard.add(
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").map {
                InlineKeyboardButton.CallbackData(it, CallbackData.CALENDAR_IGNORE)
            }
        )

        // 3. Дни месяца
        val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1
        val calendarDays = mutableListOf<InlineKeyboardButton>()

        // Добавляем пустые кнопки для смещения
        repeat(dayOfWeekOffset) {
            calendarDays.add(InlineKeyboardButton.CallbackData(" ", CallbackData.CALENDAR_IGNORE))
        }

        // Добавляем кнопки с числами
        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            // Не даем выбрать прошедшие даты
            if (date.isBefore(LocalDate.now())) {
                calendarDays.add(InlineKeyboardButton.CallbackData(" ", CallbackData.CALENDAR_IGNORE))
            } else {
                calendarDays.add(InlineKeyboardButton.CallbackData(day.toString(), "${CallbackData.CALENDAR_DAY_PREFIX}$date"))
            }
        }

        // Группируем по 7 дней в ряду
        keyboard.addAll(calendarDays.chunked(7))

        return InlineKeyboardMarkup.create(keyboard)
    }
}
