package com.example.loginpageproject.auth

import java.time.Duration
import java.time.Instant

data class FailedLoginState(
    val failures: Int = 0,
    val lockedUntil: Instant? = null
)

/** Mirrors the server policy for deterministic unit tests and UI messaging. */
object LoginLockoutPolicy {
    const val MAX_ATTEMPTS = 3
    val LOCKOUT_DURATION: Duration = Duration.ofMinutes(5)

    fun isLocked(state: FailedLoginState, now: Instant): Boolean = state.lockedUntil?.isAfter(now) == true

    fun recordFailure(state: FailedLoginState, now: Instant): FailedLoginState {
        val activeFailures = if (state.lockedUntil != null && !state.lockedUntil.isAfter(now)) 0 else state.failures
        val nextFailures = activeFailures + 1
        return if (nextFailures >= MAX_ATTEMPTS) {
            FailedLoginState(nextFailures, now.plus(LOCKOUT_DURATION))
        } else {
            FailedLoginState(nextFailures, null)
        }
    }
}

object PasswordHistoryPolicy {
    fun isReused(candidate: String, priorPasswords: Collection<String>): Boolean = candidate in priorPasswords
}
