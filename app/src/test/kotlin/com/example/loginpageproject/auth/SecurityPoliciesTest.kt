package com.example.loginpageproject.auth

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityPoliciesTest {
    @Test fun `third failed login locks the account for five minutes`() {
        val now = Instant.parse("2026-08-29T10:00:00Z")
        var state = FailedLoginState()
        repeat(2) { state = LoginLockoutPolicy.recordFailure(state, now) }
        assertFalse(LoginLockoutPolicy.isLocked(state, now))
        state = LoginLockoutPolicy.recordFailure(state, now)
        assertTrue(LoginLockoutPolicy.isLocked(state, now))
        assertFalse(LoginLockoutPolicy.isLocked(state, now.plusSeconds(301)))
    }

    @Test fun `previous passwords are rejected`() {
        val previous = listOf("OlderPassword1!", "CurrentPassword2!")
        assertTrue(PasswordHistoryPolicy.isReused("OlderPassword1!", previous))
        assertFalse(PasswordHistoryPolicy.isReused("DifferentPassword3!", previous))
    }
}
