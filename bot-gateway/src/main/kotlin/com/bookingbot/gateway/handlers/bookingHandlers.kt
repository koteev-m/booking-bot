package com.bookingbot.gateway.handlers

import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.ClubService
import com.bookingbot.api.services.TableService
import com.bookingbot.gateway.fsm.State
import com.bookingbot.gateway.fsm.StateStorage
import com.bookingbot.gateway.markup.CalendarKeyboard
import com.bookingbot.gateway.markup.Menus
import com.bookingbot.gateway.util.StateFilter
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun addBookingHandlers(
    dispatcher: Dispatcher,
    clubService: ClubService,
    tableService: TableService,
    bookingService: BookingService
) {

    // Шаг 1: Пользователь нажимает "Выбрать клуб"
    dispatcher.callbackQuery("select_club") {
        val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)
        val clubs = clubService.getAllClubs()
        val clubButtons = clubs
            .map { InlineKeyboardButton.CallbackData(it.name, "show_club_${it.id}") }
            .chunked(2)
        bot.sendMessage(
            chatId,
            text = "Выберите клуб:",
            replyMarkup = InlineKeyboardMarkup.create(clubButtons)
        )
    }

    // Единый обработчик для всех callback'ов, связанных с бронированием
    dispatcher.callbackQuery {
        val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)
        val data = callbackQuery.data

        when {
            // Шаг 2: Показ меню клуба
            data.startsWith("show_club_") -> {
                val clubId = data.removePrefix("show_club_").toIntOrNull() ?: return@callbackQuery
                val club = clubService.findClubById(clubId) ?: return@callbackQuery
                bot.sendMessage(
                    chatId = chatId,
                    text = "Вы выбрали клуб: *${club.name}*",
                    parseMode = ParseMode.MARKDOWN,
                    replyMarkup = Menus.clubMenu(clubId)
                )
            }

            // Шаг 3: Начало бронирования (показ календаря)
            data.startsWith("start_booking_") -> {
                val clubId = data.removePrefix("start_booking_").toIntOrNull() ?: return@callbackQuery
                StateStorage.getContext(chatId.id).clubId = clubId
                StateStorage.setState(chatId.id, State.DateSelection)
                val today = LocalDate.now()
                bot.sendMessage(
                    chatId = chatId,
                    text = "Выберите дату:",
                    replyMarkup = CalendarKeyboard.create(today.year, today.monthValue)
                )
            }

            // Навигация по календарю
            data.startsWith("calendar_prev_") || data.startsWith("calendar_next_") -> {
                val parts = data.split("_")
                val direction = parts[1]
                val yearMonth = java.time.YearMonth.parse(parts[2])
                val newYM = if (direction == "prev") yearMonth.minusMonths(1) else yearMonth.plusMonths(1)
                bot.editMessageReplyMarkup(
                    chatId = chatId,
                    messageId = callbackQuery.message!!.messageId,
                    replyMarkup = CalendarKeyboard.create(newYM.year, newYM.monthValue)
                )
            }

            // Выбор дня в календаре
            data.startsWith("calendar_day_") -> {
                if (StateStorage.getState(chatId.id) != State.DateSelection.key) return@callbackQuery
                val date = LocalDate.parse(data.removePrefix("calendar_day_"))
                StateStorage.getContext(chatId.id).bookingDate =
                    date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                StateStorage.setState(chatId.id, State.GuestCountInput)
                bot.deleteMessage(chatId, callbackQuery.message!!.messageId)
                bot.sendMessage(chatId, text = "Вы выбрали: $date. Сколько будет гостей?")
            }

            // Шаг 6: Выбор стола
            data.startsWith("table_") -> {
                if (StateStorage.getState(chatId.id) != State.TableSelection.key) return@callbackQuery
                val tableId = data.removePrefix("table_").toInt()
                val ctx = StateStorage.getContext(chatId.id)
                ctx.tableId = tableId

                val club = clubService.findClubById(ctx.clubId!!)
                val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
                val text = """
                    Пожалуйста, подтвердите вашу бронь:
                    - *Клуб:* ${club?.name ?: "Неизвестно"}
                    - *Стол ID:* ${ctx.tableId}
                    - *Гостей:* ${ctx.guestCount}
                    - *Дата:* ${fmt.format(ctx.bookingDate!!)}
                """.trimIndent()
                val buttons = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData("✅ Подтвердить", "confirm_booking"),
                        InlineKeyboardButton.CallbackData("❌ Отмена", "cancel_booking_fsm")
                    )
                )
                StateStorage.setState(chatId.id, State.Confirmation)
                bot.editMessageText(
                    chatId = chatId,
                    messageId = callbackQuery.message!!.messageId,
                    text = text,
                    replyMarkup = buttons,
                    parseMode = ParseMode.MARKDOWN
                )
            }

            // Шаг 7: Финальное подтверждение и уведомление администратору
            data == "confirm_booking" -> {
                if (StateStorage.getState(chatId.id) != State.Confirmation.key) return@callbackQuery
                val ctx = StateStorage.getContext(chatId.id)
                val request = BookingRequest(
                    userId = chatId.id,
                    clubId = ctx.clubId!!,
                    tableId = ctx.tableId!!,
                    bookingTime = ctx.bookingDate!!,
                    partySize = ctx.guestCount!!,
                    bookingGuestName = ctx.bookingGuestName,
                    promoterId = ctx.promoterId,
                    source = ctx.source ?: "Бот",
                    phone = ctx.phone,
                    telegramId = chatId.id
                )
                val booking = bookingService.createBooking(request)

                // 1) Отвечаем пользователю
                bot.editMessageText(
                    chatId = chatId,
                    messageId = callbackQuery.message!!.messageId,
                    text = "Отлично! Ваша бронь №${booking.id} подтверждена."
                )

                // 2) Уведомляем администратора клуба
                val club = clubService.findClubById(ctx.clubId!!)
                club?.adminChannelId?.let { channelId ->
                    val fmtDateTime = DateTimeFormatter
                        .ofPattern("dd.MM.yyyy HH:mm")
                        .withZone(ZoneId.systemDefault())
                    val notifyText = """
                        🔔 *Новая бронь!*

                        *ID брони:* ${booking.id}
                        *Гость:* @${booking.bookingGuestName ?: "N/A"}
                        *Кол-во человек:* ${booking.partySize}
                        *Стол ID:* ${booking.tableId}
                        *Время:* ${fmtDateTime.format(booking.bookingTime)}
                    """.trimIndent()
                    val adminButtons = InlineKeyboardMarkup.create(
                        listOf(
                            InlineKeyboardButton.CallbackData("✅ Гости пришли", "admin_confirm_${booking.id}"),
                            InlineKeyboardButton.CallbackData("❌ Неявка", "admin_noshow_${booking.id}")
                        )
                    )
                    bot.sendMessage(
                        chatId = ChatId.fromId(channelId),
                        text = notifyText,
                        parseMode = ParseMode.MARKDOWN,
                        replyMarkup = adminButtons
                    )
                }

                StateStorage.clear(chatId.id)
            }

            // Отмена в процессе FSM
            data == "cancel_booking_fsm" -> {
                bot.editMessageText(
                    chatId = ChatId.fromId(callbackQuery.message!!.chat.id),
                    messageId = callbackQuery.message!!.messageId,
                    text = "Бронирование отменено."
                )
                StateStorage.clear(chatId.id)
            }
        }
    }

    // Шаг 5: Пользователь ввёл количество гостей
    dispatcher.message(Filter.Text and StateFilter(State.GuestCountInput.key)) {
        val chatId = ChatId.fromId(message.chat.id)
        val count = message.text?.toIntOrNull()
        if (count == null || count <= 0) {
            bot.sendMessage(chatId, text = "Пожалуйста, введите корректное число гостей.")
            return@message
        }
        StateStorage.getContext(chatId.id).guestCount = count
        StateStorage.setState(chatId.id, State.ContactInput)
        bot.sendMessage(chatId, text = "Отлично. Теперь введите, пожалуйста, ваш номер телефона:")
    }

    // Шаг 6: Пользователь ввёл телефон
    dispatcher.message(Filter.Text and StateFilter(State.ContactInput.key)) {
        val chatId = ChatId.fromId(message.chat.id)
        val phone = message.text
        val phoneRegex = """^\+?\d{10,14}$""".toRegex()
        if (phone == null || !phone.matches(phoneRegex)) {
            bot.sendMessage(chatId, text = "Неверный формат. Введите номер, например: +79991234567")
            return@message
        }
        val ctx = StateStorage.getContext(chatId.id)
        ctx.phone = phone

        val available = tableService.getAvailableTables(ctx.clubId!!, ctx.bookingDate!!, ctx.guestCount!!)
        if (available.isEmpty()) {
            bot.sendMessage(chatId, text = "Нет свободных столов для указанного кол-ва гостей.")
            StateStorage.clear(chatId.id)
            return@message
        }
        val buttons = available
            .map { InlineKeyboardButton.CallbackData("Стол №${it.number} (до ${it.capacity} чел.)", "table_${it.id}") }
            .chunked(2)
        StateStorage.setState(chatId.id, State.TableSelection)
        bot.sendMessage(chatId, text = "Выберите стол:", replyMarkup = InlineKeyboardMarkup.create(buttons))
    }
}