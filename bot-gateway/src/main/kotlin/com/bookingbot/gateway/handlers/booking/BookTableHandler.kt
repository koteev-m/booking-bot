package com.bookingbot.gateway.handlers.booking

import com.bookingbot.api.services.BookingService
import com.bookingbot.api.services.ClubService
import com.bookingbot.api.services.TableService
import com.bookingbot.gateway.TelegramApi
import com.bookingbot.gateway.fsm.State
import com.bookingbot.gateway.fsm.StateStorageImpl
import com.bookingbot.gateway.hall.HallSchemeRenderer
import com.bookingbot.gateway.util.CallbackData
import com.bookingbot.gateway.util.StateFilter
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.files.TelegramFile
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.bookingbot.gateway.markup.CalendarKeyboard
import com.bookingbot.gateway.markup.Menus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Booking flow handlers extracted from monolith file. */
object BookTableHandler : KoinComponent {
    private val clubService: ClubService by inject()
    private val tableService: TableService by inject()
    private val bookingService: BookingService by inject()
    private val schemeRenderer: HallSchemeRenderer by inject()

    /** Register booking handlers on [dispatcher]. */
    fun register(dispatcher: Dispatcher) {
        dispatcher.callbackQuery(CallbackData.SELECT_CLUB) {
            val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)
            val clubs = clubService.getAllClubs()
            val clubButtons = clubs.map {
                InlineKeyboardButton.CallbackData(it.name, "${CallbackData.SHOW_CLUB_PREFIX}${it.id}")
            }.chunked(2)
            TelegramApi.sendMessage(chatId, text = "Выберите клуб:", replyMarkup = InlineKeyboardMarkup.create(clubButtons))
        }

        dispatcher.callbackQuery {
            val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)
            val data = callbackQuery.data ?: return@callbackQuery
            when {
                data.startsWith(CallbackData.SHOW_CLUB_PREFIX) -> {
                    val clubId = data.removePrefix(CallbackData.SHOW_CLUB_PREFIX).toInt()
                    val club = clubService.findClubById(clubId) ?: return@callbackQuery
                    TelegramApi.sendMessage(
                        chatId = chatId,
                        text = "Вы выбрали клуб: *${club.name}*",
                        parseMode = ParseMode.MARKDOWN,
                        replyMarkup = Menus.clubMenu(clubId)
                    )
                }
                data.startsWith(CallbackData.START_BOOKING_PREFIX) -> {
                    val clubId = data.removePrefix(CallbackData.START_BOOKING_PREFIX).toInt()
                    StateStorageImpl.getContext(chatId.id).clubId = clubId
                    StateStorageImpl.saveState(chatId.id, State.DateSelection)
                    val today = LocalDate.now()
                    val calendarMarkup = CalendarKeyboard.create(today.year, today.monthValue)
                    val keyboardWithBack = calendarMarkup.inlineKeyboard.toMutableList().apply {
                        add(listOf(Menus.backToMainMenuButton))
                    }
                    TelegramApi.sendMessage(
                        chatId = chatId,
                        text = "Выберите дату (или введите /cancel для отмены):",
                        replyMarkup = InlineKeyboardMarkup.create(keyboardWithBack)
                    )
                }
                data.startsWith(CallbackData.TABLE_PREFIX) -> {
                    if (StateStorageImpl.getState(chatId.id) != State.TableSelection) return@callbackQuery
                    val tableId = data.removePrefix(CallbackData.TABLE_PREFIX).toInt()
                    val context = StateStorageImpl.getContext(chatId.id)
                    context.tableId = tableId
                    val club = clubService.findClubById(context.clubId!!)
                    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
                    val depositAmount = tableService.calculateDeposit(tableId, context.guestCount!!)

                    val confirmationText = """
                        Пожалуйста, подтвердите вашу бронь:
                        - *Клуб:* ${club?.name ?: "Неизвестно"}
                        - *Стол ID:* ${context.tableId}
                        - *Гостей:* ${context.guestCount}
                        - *Дата:* ${formatter.format(context.bookingDate!!)}
                        - *Депозит:* ${depositAmount.toInt()} руб.
                    """.trimIndent()

                    val confirmationButtons = InlineKeyboardMarkup.create(
                        listOf(
                            InlineKeyboardButton.CallbackData("✅ Подтвердить", CallbackData.CONFIRM_BOOKING),
                            InlineKeyboardButton.CallbackData("❌ Отмена", CallbackData.CANCEL_BOOKING_FSM)
                        )
                    )
                    StateStorageImpl.saveState(chatId.id, State.Confirmation)
                    bot.editMessageText(
                        chatId = chatId,
                        messageId = callbackQuery.message!!.messageId,
                        text = confirmationText,
                        replyMarkup = confirmationButtons,
                        parseMode = ParseMode.MARKDOWN
                    )
                }
                data == CallbackData.CONFIRM_BOOKING -> {
                    if (StateStorageImpl.getState(chatId.id) != State.Confirmation) return@callbackQuery
                    val context = StateStorageImpl.getContext(chatId.id)
                    val request = BookingRequest(
                        userId = chatId.id,
                        clubId = context.clubId!!,
                        tableId = context.tableId!!,
                        bookingTime = context.bookingDate!!,
                        partySize = context.guestCount!!,
                        bookingGuestName = callbackQuery.from.username,
                        promoterId = context.promoterId,
                        bookingSource = context.source ?: "Бот",
                        phone = context.phone,
                        telegramId = chatId.id
                    )
                    val booking = bookingService.createBooking(request)
                    bot.editMessageText(chatId, callbackQuery.message!!.messageId, text = "Отлично! Ваша бронь №${booking.id} подтверждена.")
                    StateStorageImpl.clearState(chatId.id)
                }
                data == CallbackData.CANCEL_BOOKING_FSM -> {
                    bot.editMessageText(chatId, callbackQuery.message!!.messageId, text = "Бронирование отменено.")
                    StateStorageImpl.clearState(chatId.id)
                }
            }
        }

        dispatcher.message(Filter.Text and StateFilter(State.GuestCountInput)) {
            val chatId = ChatId.fromId(message.chat.id)
            val guestCount = message.text?.toIntOrNull()
            if (guestCount == null || guestCount <= 0) {
                TelegramApi.sendMessage(chatId, text = "Пожалуйста, введите корректное число гостей.")
                return@message
            }
            val context = StateStorageImpl.getContext(chatId.id)
            context.guestCount = guestCount
            StateStorageImpl.saveState(chatId.id, State.ContactInput)
            TelegramApi.sendMessage(chatId, text = "Отлично. Теперь, пожалуйста, введите ваш контактный номер телефона:")
        }

        dispatcher.message(Filter.Text and StateFilter(State.ContactInput)) {
            val chatId = ChatId.fromId(message.chat.id)
            val phone = message.text ?: return@message
            val phoneRegex = """^\+?\d{10,14}$""".toRegex()
            if (!phone.matches(phoneRegex)) {
                TelegramApi.sendMessage(chatId, text = "Неверный формат номера. Пожалуйста, введите номер в международном формате, например: +79991234567")
                return@message
            }
            val context = StateStorageImpl.getContext(chatId.id)
            context.phone = phone
            val tables = tableService.getAvailableTables(context.clubId!!, context.bookingDate!!, context.guestCount!!)
            if (tables.isEmpty()) {
                TelegramApi.sendMessage(chatId, "К сожалению, нет свободных столов на указанное количество гостей.")
                StateStorageImpl.clearState(chatId.id)
                return@message
            }
            StateStorageImpl.saveState(chatId.id, State.TableSelection)
            val bytes = schemeRenderer.render(tables.map { it.id })
            bot.sendPhoto(chatId, TelegramFile.ByByteArray(bytes, "hall.png"))
        }
    }
}
