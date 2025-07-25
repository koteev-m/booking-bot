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
import com.bookingbot.gateway.util.CallbackData
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
    dispatcher.callbackQuery(CallbackData.SELECT_CLUB) {
        val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)
        val clubs = clubService.getAllClubs()
        val clubButtons = clubs.map { InlineKeyboardButton.CallbackData(it.name, "${CallbackData.SHOW_CLUB_PREFIX}${it.id}") }.chunked(2)
        bot.sendMessage(chatId, text = "Выберите клуб:", replyMarkup = InlineKeyboardMarkup.create(clubButtons))
    }

    // Единый обработчик для всех callback'ов, связанных с бронированием
    dispatcher.callbackQuery {
        val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)
        val data = callbackQuery.data

        when {
            // Шаг 2: Показ меню клуба
            data.startsWith(CallbackData.SHOW_CLUB_PREFIX) -> {
                val clubId = data.removePrefix(CallbackData.SHOW_CLUB_PREFIX).toIntOrNull() ?: return@callbackQuery
                val club = clubService.findClubById(clubId) ?: return@callbackQuery
                bot.sendMessage(
                    chatId = chatId,
                    text = "Вы выбрали клуб: *${club.name}*",
                    parseMode = ParseMode.MARKDOWN,
                    replyMarkup = Menus.clubMenu(clubId)
                )
            }

            // Шаг 3: Начало бронирования (показ календаря)
            data.startsWith(CallbackData.START_BOOKING_PREFIX) -> {
                val clubId = data.removePrefix(CallbackData.START_BOOKING_PREFIX).toIntOrNull() ?: return@callbackQuery
                StateStorage.getContext(chatId.id).clubId = clubId
                StateStorage.setState(chatId.id, State.DateSelection)
                val today = LocalDate.now()

                val calendarMarkup = CalendarKeyboard.create(today.year, today.monthValue)
                val keyboardWithBack = calendarMarkup.inlineKeyboard.toMutableList().apply {
                    add(listOf(Menus.backToMainMenuButton))
                }
                bot.sendMessage(
                    chatId = chatId,
                    text = "Выберите дату (или введите /cancel для отмены):",
                    replyMarkup = InlineKeyboardMarkup.create(keyboardWithBack)
                )
            }

            // Навигация по календарю
            data.startsWith(CallbackData.CALENDAR_PREV_PREFIX) || data.startsWith(CallbackData.CALENDAR_NEXT_PREFIX) -> {
                val parts = data.split("_")
                val direction = parts[1]
                val yearMonth = java.time.YearMonth.parse(parts[2])
                val newYearMonth = if (direction == "prev") yearMonth.minusMonths(1) else yearMonth.plusMonths(1)

                val calendarMarkup = CalendarKeyboard.create(newYearMonth.year, newYearMonth.monthValue)
                val keyboardWithBack = calendarMarkup.inlineKeyboard.toMutableList().apply {
                    add(listOf(Menus.backToMainMenuButton))
                }

                bot.editMessageReplyMarkup(
                    chatId = chatId,
                    messageId = callbackQuery.message!!.messageId,
                    replyMarkup = InlineKeyboardMarkup.create(keyboardWithBack)
                )
            }

            // Выбор дня в календаре
            data.startsWith(CallbackData.CALENDAR_DAY_PREFIX) -> {
                if (StateStorage.getState(chatId.id) != State.DateSelection.key) return@callbackQuery
                val date = LocalDate.parse(data.removePrefix(CallbackData.CALENDAR_DAY_PREFIX))
                StateStorage.getContext(chatId.id).bookingDate = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                StateStorage.setState(chatId.id, State.GuestCountInput)
                bot.deleteMessage(chatId, callbackQuery.message!!.messageId)
                bot.sendMessage(chatId, text = "Вы выбрали: $date. Сколько будет гостей?")
            }

            // Шаг 6: Выбор стола
            data.startsWith(CallbackData.TABLE_PREFIX) -> {
                if (StateStorage.getState(chatId.id) != State.TableSelection.key) return@callbackQuery
                val tableId = data.removePrefix(CallbackData.TABLE_PREFIX).toInt()
                val context = StateStorage.getContext(chatId.id)
                context.tableId = tableId

                val club = clubService.findClubById(context.clubId!!)
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
                val depositAmount = tableService.calculateDeposit(tableId, context.guestCount!!)

                val staffInfo = if (context.source != null && context.source != "Бот") {
                    """
                    - *Имя гостя:* ${context.bookingGuestName ?: "Не указано"}
                    - *Телефон:* ${context.phone ?: "Не указан"}
                    - *Источник:* ${context.source}
                    """.trimIndent()
                } else {
                    ""
                }

                val confirmationText = """
                    Пожалуйста, подтвердите вашу бронь:
                    - *Клуб:* ${club?.name ?: "Неизвестно"}
                    - *Стол ID:* ${context.tableId}
                    - *Гостей:* ${context.guestCount}
                    - *Дата:* ${formatter.format(context.bookingDate!!)}
                    - *Депозит:* ${depositAmount.toInt()} руб.
                    $staffInfo
                """.trimIndent()

                val confirmationButtons = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData("✅ Подтвердить", CallbackData.CONFIRM_BOOKING),
                        InlineKeyboardButton.CallbackData("❌ Отмена", CallbackData.CANCEL_BOOKING_FSM)
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
            data == CallbackData.CONFIRM_BOOKING -> {
                if (StateStorage.getState(chatId.id) != State.Confirmation.key) return@callbackQuery
                val context = StateStorage.getContext(chatId.id)

                val guestName = if (context.promoterId != null || (context.source != null && context.source != "Бот")) {
                    context.bookingGuestName
                } else {
                    callbackQuery.from.username
                }

                val request = BookingRequest(
                    userId = chatId.id,
                    clubId = context.clubId!!,
                    tableId = context.tableId!!,
                    bookingTime = context.bookingDate!!,
                    partySize = context.guestCount!!,
                    bookingGuestName = guestName,
                    promoterId = context.promoterId,
                    bookingSource = context.source ?: "Бот",
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
                            InlineKeyboardButton.CallbackData("✅ Гости пришли", "${CallbackData.ADMIN_CONFIRM_PREFIX}${booking.id}"),
                            InlineKeyboardButton.CallbackData("❌ Неявка", "${CallbackData.ADMIN_NOSHOW_PREFIX}${booking.id}"),
                            InlineKeyboardButton.CallbackData("🚫 Отменить (Менеджер)", "${CallbackData.ADMIN_CANCEL_PREFIX}${booking.id}")
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
                    *Источник:* ${booking.bookingSource}
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
            data == CallbackData.CANCEL_BOOKING_FSM -> {
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

        val tableButtons = tables.map {
            val deposit = tableService.calculateDeposit(it.id, context.guestCount!!)
            InlineKeyboardButton.CallbackData(
                "Стол №${it.number} (до ${it.capacity} чел., депозит от ${deposit.toInt()} руб.)",
                "${CallbackData.TABLE_PREFIX}${it.id}"
            )
        }.chunked(2)
        StateStorage.setState(chatId.id, State.TableSelection)
        bot.sendMessage(chatId, text = "Спасибо! Выберите стол:", replyMarkup = InlineKeyboardMarkup.create(tableButtons))
    }
}