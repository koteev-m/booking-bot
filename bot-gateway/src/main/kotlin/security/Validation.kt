package security

import io.ktor.server.plugins.requestvalidation.ValidationResult

/**
 * Collection of validation helper functions.
 */
object ValidationRules {
    private val phoneRegex = Regex("^\\+?\\d{10,14}$")

    fun validatePhone(phone: String): ValidationResult =
        if (phoneRegex.matches(phone)) ValidationResult.Valid
        else ValidationResult.Invalid("Invalid phone format")

    fun validateGuestName(name: String): ValidationResult =
        if (name.isBlank()) ValidationResult.Invalid("Guest name is blank")
        else ValidationResult.Valid
}
