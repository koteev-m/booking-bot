package com.bookingbot.api.services

/**
 * Listener for booking changes that may affect waitlist entries.
 */
interface WaitlistNotifier {
    fun onNewBooking()
    fun onCancel()
}

object WaitlistNotifierHolder {
    var notifier: WaitlistNotifier = object : WaitlistNotifier {
        override fun onNewBooking() {}
        override fun onCancel() {}
    }
}
