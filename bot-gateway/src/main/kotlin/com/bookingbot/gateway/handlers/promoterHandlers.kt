package com.bookingbot.gateway.handlers
import com.bookingbot.gateway.TelegramApi

import com.bookingbot.api.model.UserRole
import com.bookingbot.api.services.ClubService
import com.bookingbot.api.services.UserService
import com.bookingbot.gateway.fsm.State
import com.bookingbot.gateway.fsm.StateStorageImpl
import com.bookingbot.gateway.util.StateFilter
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.bookingbot.gateway.util.CallbackData
import com.github.kotlintelegrambot.extensions.filters.Filter

fun addPromoterHandlers(dispatcher: Dispatcher, userService: UserService, clubService: ClubService) {

    // Шаг 1: Промоутер нажимает "Бронь для гостя"
    dispatcher.callbackQuery(CallbackData.PROMOTER_START_BOOKING) {
        val promoterId = callbackQuery.from.id
        val promoter = userService.findOrCreateUser(promoterId, callbackQuery.from.username)

        // Проверяем, что у пользователя роль промоутера
        if (promoter.role != UserRole.PROMOTER && promoter.role != UserRole.OWNER) {
            bot.answerCallbackQuery(callbackQuery.id, text = "Эта функция доступна только для промоутеров.", showAlert = true)
            return@callbackQuery
        }

        // Устанавливаем состояние ожидания имени гостя
        StateStorageImpl.saveState(promoterId, State.PromoterGuestNameInput)
        TelegramApi.sendMessage(
            chatId = ChatId.fromId(promoterId),
            text = "Вы начали создание брони для гостя. Пожалуйста, введите имя гостя:"
        )
    }

    // Шаг 2: Промоутер вводит имя гостя
    dispatcher.message(Filter.Text and StateFilter(State.PromoterGuestNameInput)) {
        val promoterId = message.from?.id ?: return@message
        val guestName = message.text ?: return@message
        val promoter = userService.findOrCreateUser(promoterId, message.from?.username)

        val context = StateStorageImpl.getContext(promoterId)
        // Сохраняем имя гостя, ID промоутера и источник брони в контекст
        context.bookingGuestName = guestName
        context.promoterId = promoterId
        context.source = promoter.username ?: "Promoter#$promoterId"

        // Перенаправляем промоутера на стандартный флоу выбора клуба
        val clubs = clubService.getAllClubs()
        val clubButtons = clubs.map {
            InlineKeyboardButton.CallbackData(it.name, "${CallbackData.SHOW_CLUB_PREFIX}${it.id}")
        }.chunked(2)

        TelegramApi.sendMessage(
            chatId = ChatId.fromId(promoterId),
            text = "Имя гостя '$guestName' принято. Теперь выберите клуб:",
            replyMarkup = InlineKeyboardMarkup.create(clubButtons)
        )

        // Переводим промоутера в состояние выбора клуба, чтобы FSM гостя подхватил диалог
        StateStorageImpl.saveState(promoterId, State.ClubSelection)
    }
}
