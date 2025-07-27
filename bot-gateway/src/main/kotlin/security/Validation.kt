package security

import io.ktor.server.plugins.requestvalidation.ValidationException

/**
 * Collection of validation helper functions.
 */
object ValidationRules {
    private val phoneRegex = Regex("^\\+?\\d{10,14}$")

    fun validatePhone(phone: String) {
        if (!phoneRegex.matches(phone)) {
            throw ValidationException("Invalid phone format")
        }
    }

    fun validateGuestName(name: String) {
        if (name.isBlank()) {
            throw ValidationException("Guest name is blank")
        }
    }
}
