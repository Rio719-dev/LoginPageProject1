package com.example.loginpageproject.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegistrationData(
    @SerialName("full_name") val fullName: String,
    val birthday: String,
    val address: String,
    val email: String,
    val mobile: String,
    @SerialName("requested_role") val requestedRole: String,
    val password: String
)

enum class AccessRole(val databaseValue: String, val displayName: String) {
    SUPER_ADMIN("super_admin", "Super Admin"),
    ADMIN("admin", "Admin"),
    USER("user", "User");

    companion object {
        fun fromDatabase(value: String?): AccessRole = entries.firstOrNull { it.databaseValue == value } ?: USER
    }
}

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String,
    val birthday: String,
    val address: String,
    val mobile: String,
    val role: String,
    @SerialName("requested_role") val requestedRole: String? = null,
    @SerialName("must_change_password") val mustChangePassword: Boolean = false
) {
    val accessRole: AccessRole get() = AccessRole.fromDatabase(role)
}

sealed interface LoginResult {
    data class Success(val profile: UserProfile) : LoginResult
    data class Locked(val secondsRemaining: Long) : LoginResult
    data object InvalidCredentials : LoginResult
}

class AuthConfigurationException(message: String) : IllegalStateException(message)
class PasswordReuseException : IllegalStateException("You cannot reuse a previous password. Please choose a new password.")
