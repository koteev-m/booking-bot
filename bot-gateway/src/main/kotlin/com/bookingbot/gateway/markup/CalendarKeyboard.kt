package com.bookingbot.gateway.markup

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
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
                InlineKeyboardButton.CallbackData("◀", "calendar_prev_${yearMonth}"),
                InlineKeyboardButton.CallbackData("$monthName $year", "calendar_ignore"),
                InlineKeyboardButton.CallbackData("▶", "calendar_next_${yearMonth}")
            )
        )

        // 2. Дни недели
        keyboard.add(
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").map {
                InlineKeyboardButton.CallbackData(it, "calendar_ignore")
            }
        )

        // 3. Дни месяца
        val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1
        val calendarDays = mutableListOf<InlineKeyboardButton>()

        // Добавляем пустые кнопки для смещения
        repeat(dayOfWeekOffset) {
            calendarDays.add(InlineKeyboardButton.CallbackData(" ", "calendar_ignore"))
        }

        // Добавляем кнопки с числами
        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            // Не даем выбрать прошедшие даты
            if (date.isBefore(LocalDate.now())) {
                calendarDays.add(InlineKeyboardButton.CallbackData(" ", "calendar_ignore"))
            } else {
                calendarDays.add(InlineKeyboardButton.CallbackData(day.toString(), "calendar_day_$date"))
            }
        }

        // Группируем по 7 дней в ряду
        keyboard.addAll(calendarDays.chunked(7))

        return InlineKeyboardMarkup.create(keyboard)
    }
}
