package com.bookingbot.gateway.fsm

// Состояния, через которые проходит пользователь
sealed class State(val key: String) {
    // Состояния для бронирования гостем
    object ClubSelection : State("club_selection")
    object DateSelection : State("date_selection")
    object GuestCountInput : State("guest_count_input")
    object ContactInput : State("contact_input")
    object TableSelection : State("table_selection")
    object Confirmation : State("confirmation")

    // Состояния для вопроса
    object AskingQuestionClub : State("asking_question_club")
    object AskingQuestionText : State("asking_question_text")

    // Состояния для промоутера
    object PromoterGuestNameInput : State("promoter_guest_name_input")

    // Состояния для админа
    object AdminBookingGuestName : State("admin_booking_guest_name")
    object AdminBookingSource : State("admin_booking_source")
}

