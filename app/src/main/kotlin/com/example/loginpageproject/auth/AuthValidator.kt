package com.example.loginpageproject.auth

import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Pure validation rules shared by registration and password flows. */
object AuthValidator {
    private val fullNamePattern = Regex("^[\\p{L}]+(?:[ '\\-’][\\p{L}]+)*$")
    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val mobilePattern = Regex("^(09\\d{9}|\\+639\\d{9})$")
    private val uppercasePattern = Regex(".*[A-Z].*")
    private val lowercasePattern = Regex(".*[a-z].*")
    private val digitPattern = Regex(".*\\d.*")
    private val specialPattern = Regex(".*[^A-Za-z0-9].*")

    fun isValidFullName(value: String): Boolean = fullNamePattern.matches(value.trim())

    fun isValidEmail(value: String): Boolean = emailPattern.matches(value.trim())

    fun isValidPhilippineMobile(value: String): Boolean = mobilePattern.matches(value.trim())

    fun parseBirthday(value: String): LocalDate? = try {
        LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        null
    }

    fun isAtLeast18(birthday: LocalDate, clock: Clock = Clock.systemDefaultZone()): Boolean {
        val today = LocalDate.now(clock)
        return !birthday.isAfter(today.minusYears(18))
    }

    fun passwordRequirements(password: String): PasswordRequirements = PasswordRequirements(
        correctLength = password.length in 8..16,
        hasUppercase = uppercasePattern.matches(password),
        hasLowercase = lowercasePattern.matches(password),
        hasNumber = digitPattern.matches(password),
        hasSpecialCharacter = specialPattern.matches(password)
    )

    fun isStrongPassword(password: String): Boolean = passwordRequirements(password).isStrong
}

data class PasswordRequirements(
    val correctLength: Boolean,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasNumber: Boolean,
    val hasSpecialCharacter: Boolean
) {
    val score: Int get() = listOf(correctLength, hasUppercase, hasLowercase, hasNumber, hasSpecialCharacter).count { it }
    val isStrong: Boolean get() = score == 5
}
