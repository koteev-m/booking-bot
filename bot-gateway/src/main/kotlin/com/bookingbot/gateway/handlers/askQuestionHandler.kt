package com.bookingbot.gateway.handlers
import com.bookingbot.gateway.TelegramApi

import com.bookingbot.api.services.ClubService
import com.bookingbot.gateway.fsm.State
import com.bookingbot.gateway.fsm.StateStorage
import com.bookingbot.gateway.util.StateFilter
import com.bookingbot.gateway.util.CallbackData
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter

fun addAskQuestionHandler(dispatcher: Dispatcher, clubService: ClubService) {

    // Шаг 1: Пользователь нажимает "Задать вопрос"
    dispatcher.callbackQuery(CallbackData.ASK_QUESTION) {
        val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)
        StateStorage.setState(chatId.id, State.AskingQuestionClub)

        val clubs = clubService.getAllClubs()
        val clubButtons = clubs.map { club ->
            InlineKeyboardButton.CallbackData(text = club.name, callbackData = "${CallbackData.ASK_CLUB_PREFIX}${club.id}")
        }.chunked(2)

        TelegramApi.sendMessage(
            chatId = chatId,
            text = "Выберите клуб, которому хотите задать вопрос:",
            replyMarkup = InlineKeyboardMarkup.create(clubButtons)
        )
    }

    // Шаг 2: Пользователь выбрал клуб, просим ввести вопрос
    dispatcher.callbackQuery {
        if (!callbackQuery.data.startsWith(CallbackData.ASK_CLUB_PREFIX)) return@callbackQuery
        if (StateStorage.getState(callbackQuery.from.id) != State.AskingQuestionClub.key) return@callbackQuery

        val chatId = ChatId.fromId(callbackQuery.message!!.chat.id)
        val clubId = callbackQuery.data.removePrefix(CallbackData.ASK_CLUB_PREFIX).toInt()

        val context = StateStorage.getContext(chatId.id)
        context.clubId = clubId // Сохраняем ID клуба в контекст

        StateStorage.setState(chatId.id, State.AskingQuestionText)
        bot.editMessageText(
            chatId = chatId,
            messageId = callbackQuery.message!!.messageId,
            text = "Теперь напишите ваш вопрос, и я перешлю его администрации."
        )
    }

    // Шаг 3: Пользователь написал вопрос, пересылаем его
    dispatcher.message(Filter.Text and StateFilter(State.AskingQuestionText.key)) {
        val chatId = ChatId.fromId(message.chat.id)
        val questionText = message.text ?: return@message
        val context = StateStorage.getContext(chatId.id)
        val club = clubService.findClubById(context.clubId!!)

        club?.adminChannelId?.let { channelId -> // <<< ИСПРАВЛЕНО
            val notification = """
                ❓ *Новый вопрос от пользователя!*
                
                *От:* @${message.from?.username ?: "N/A"} (ID: `${message.from?.id}`)
                *Вопрос:*
                ${questionText}
                
                _Чтобы ответить, используйте команду `/answer ${message.from?.id} <текст ответа>`_
            """.trimIndent()

            TelegramApi.sendMessage(
                chatId = ChatId.fromId(channelId),
                text = notification,
                parseMode = ParseMode.MARKDOWN
            )
            TelegramApi.sendMessage(chatId, "Ваш вопрос отправлен администрации клуба '${club.name}'. Ожидайте ответа.")
        } ?: TelegramApi.sendMessage(chatId, "Не удалось отправить ваш вопрос. Попробуйте позже.")

        StateStorage.clear(chatId.id)
    }
}
