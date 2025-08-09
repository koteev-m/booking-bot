package com.bookingbot.gateway.fsm

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
    object PromoterGuestListNameInput : State("promoter_guestlist_name_input")

    // Состояния для админа
    object AdminBookingGuestName : State("admin_booking_guest_name")
    object AdminBookingSource : State("admin_booking_source")
    object AdminBookingPhone : State("admin_booking_phone")

    // Состояния для управления столами
    object AdminSelectTableToEdit : State("admin_select_table_to_edit")
    object AdminEditingTableCapacity : State("admin_editing_table_capacity")
    object AdminEditingTableDeposit : State("admin_editing_table_deposit")

    // Состояния для рассылки
    object BroadcastMessageInput : State("broadcast_message_input")
    object BroadcastConfirmation : State("broadcast_confirmation")
}


