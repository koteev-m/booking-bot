package com.bookingbot.gateway.handlers

import com.bookingbot.api.model.booking.BookingRequest
import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.ClubService
import com.bookingbot.api.services.TableService
import com.bookingbot.gateway.Bot
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
        val clubButtons = clubs.map { InlineKeyboardButton.CallbackData(it.name, "show_club_${it.id}") }.chunked(2)
        bot.sendMessage(chatId, text = "Выберите клуб:", replyMarkup = InlineKeyboardMarkup.create(clubButtons))
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
                val newYearMonth = if (direction == "prev") yearMonth.minusMonths(1) else yearMonth.plusMonths(1)
                bot.editMessageReplyMarkup(
                    chatId = chatId,
                    messageId = callbackQuery.message!!.messageId,
                    replyMarkup = CalendarKeyboard.create(newYearMonth.year, newYearMonth.monthValue)
                )
            }

            // Выбор дня в календаре
            data.startsWith("calendar_day_") -> {
                if (StateStorage.getState(chatId.id) != State.DateSelection.key) return@callbackQuery
                val date = LocalDate.parse(data.removePrefix("calendar_day_"))
                StateStorage.getContext(chatId.id).bookingDate = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                StateStorage.setState(chatId.id, State.GuestCountInput)
                bot.deleteMessage(chatId, callbackQuery.message!!.messageId)
                bot.sendMessage(chatId, text = "Вы выбрали: $date. Сколько будет гостей?")
            }

            // Шаг 6: Выбор стола
            data.startsWith("table_") -> {
                if (StateStorage.getState(chatId.id) != State.TableSelection.key) return@callbackQuery
                val tableId = data.removePrefix("table_").toInt()
                val context = StateStorage.getContext(chatId.id)
                context.tableId = tableId

                val club = clubService.findClubById(context.clubId!!)
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
                val confirmationText = """
                    Пожалуйста, подтвердите вашу бронь:
                    - *Клуб:* ${club?.name ?: "Неизвестно"}
                    - *Стол ID:* ${context.tableId}
                    - *Гостей:* ${context.guestCount}
                    - *Дата:* ${formatter.format(context.bookingDate!!)}
                """.trimIndent()
                val confirmationButtons = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData("✅ Подтвердить", "confirm_booking"),
                        InlineKeyboardButton.CallbackData("❌ Отмена", "cancel_booking_fsm")
                    )
                )
                StateStorage.setState(chatId.id, State.Confirmation)
                bot.editMessageText(
                    chatId = chatId,
                    messageId = callbackQuery.message!!.messageId,
                    text = confirmationText,
                    replyMarkup = confirmationButtons,
                    parseMode = ParseMode.MARKDOWN
                )
            }

            // Шаг 7: Финальное подтверждение
            data == "confirm_booking" -> {
                if (StateStorage.getState(chatId.id) != State.Confirmation.key) return@callbackQuery
                val context = StateStorage.getContext(chatId.id)
                val request = BookingRequest(
                    userId = chatId.id,
                    clubId = context.clubId!!,
                    tableId = context.tableId!!,
                    bookingTime = context.bookingDate!!,
                    partySize = context.guestCount!!,
                    bookingGuestName = context.bookingGuestName ?: callbackQuery.from.username,
                    promoterId = context.promoterId,
                    source = context.source ?: "Бот",
                    phone = context.phone,
                    telegramId = chatId.id
                )
                val booking = bookingService.createBooking(request)
                bot.editMessageText(chatId, callbackQuery.message!!.messageId, text = "Отлично! Ваша бронь №${booking.id} подтверждена.")

                val club = clubService.findClubById(context.clubId!!)
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())

                // 1. Уведомление для менеджеров КОНКРЕТНОГО клуба
                club?.adminChannelId?.let { channelId ->
                    val notificationText = """
                        🔔 *Новая бронь!*
                        *Клуб:* ${club.name}
                        *ID брони:* ${booking.id}
                        *Гость:* @${booking.bookingGuestName ?: "N/A"}
                        *Кол-во человек:* ${booking.partySize}
                        *Стол ID:* ${booking.tableId}
                        *Время:* ${formatter.format(booking.bookingTime)}
                    """.trimIndent()

                    val adminKeyboard = InlineKeyboardMarkup.create(
                        listOf(
                            InlineKeyboardButton.CallbackData("✅ Гости пришли", "admin_confirm_${booking.id}"),
                            InlineKeyboardButton.CallbackData("❌ Неявка", "admin_noshow_${booking.id}"),
                            InlineKeyboardButton.CallbackData("🚫 Отменить (Менеджер)", "admin_cancel_${booking.id}")
                        )
                    )

                    bot.sendMessage(
                        chatId = ChatId.fromId(channelId),
                        text = notificationText,
                        parseMode = ParseMode.MARKDOWN,
                        replyMarkup = adminKeyboard
                    )
                }

                // 2. Уведомление для УПРАВЛЯЮЩИХ (в общий канал)
                val generalNotificationText = """
                    🌐 *Новая бронь (Общая сводка)*
                    
                    *Клуб:* ${club?.name ?: "Неизвестно"}
                    *ID брони:* ${booking.id}
                    *Источник:* ${booking.source}
                    *Гость:* @${booking.bookingGuestName ?: "N/A"}
                    *Кол-во человек:* ${booking.partySize}
                    *Стол ID:* ${booking.tableId}
                    *Время:* ${formatter.format(booking.bookingTime)}
                """.trimIndent()

                bot.sendMessage(
                    chatId = ChatId.fromId(Bot.GENERAL_ADMIN_CHANNEL_ID),
                    text = generalNotificationText,
                    parseMode = ParseMode.MARKDOWN
                )

                StateStorage.clear(chatId.id)
            }

            // Отмена в процессе FSM
            data == "cancel_booking_fsm" -> {
                bot.editMessageText(chatId, callbackQuery.message!!.messageId, text = "Бронирование отменено.")
                StateStorage.clear(chatId.id)
            }
        }
    }

    // Шаг 5: Пользователь ввел количество гостей, просим телефон
    dispatcher.message(Filter.Text and StateFilter(State.GuestCountInput.key)) {
        val chatId = ChatId.fromId(message.chat.id)
        val guestCount = message.text?.toIntOrNull()
        if (guestCount == null || guestCount <= 0) {
            bot.sendMessage(chatId, text = "Пожалуйста, введите корректное число гостей.")
            return@message
        }
        val context = StateStorage.getContext(chatId.id)
        context.guestCount = guestCount

        StateStorage.setState(chatId.id, State.ContactInput)
        bot.sendMessage(chatId, text = "Отлично. Теперь, пожалуйста, введите ваш контактный номер телефона:")
    }

    // Шаг 6: Пользователь ввел телефон, показываем столы
    dispatcher.message(Filter.Text and StateFilter(State.ContactInput.key)) {
        val chatId = ChatId.fromId(message.chat.id)
        val phone = message.text

        // Простая валидация номера телефона
        val phoneRegex = """^\+?\d{10,14}$""".toRegex()
        if (phone == null || !phone.matches(phoneRegex)) {
            bot.sendMessage(chatId, text = "Неверный формат номера. Пожалуйста, введите номер в международном формате, например: +79991234567")
            return@message
        }

        val context = StateStorage.getContext(chatId.id)
        context.phone = phone

        val tables = tableService.getAvailableTables(context.clubId!!, context.bookingDate!!, context.guestCount!!)
        if (tables.isEmpty()) {
            bot.sendMessage(chatId, "К сожалению, нет свободных столов на указанное количество гостей.")
            StateStorage.clear(chatId.id)
            return@message
        }

        val tableButtons = tables.map { InlineKeyboardButton.CallbackData("Стол №${it.number} (до ${it.capacity} чел.)", "table_${it.id}") }.chunked(2)
        StateStorage.setState(chatId.id, State.TableSelection)
        bot.sendMessage(chatId, text = "Спасибо! Выберите стол:", replyMarkup = InlineKeyboardMarkup.create(tableButtons))
    }
}