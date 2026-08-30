package com.example.loginpageproject.auth

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {
    private val clock = Clock.fixed(Instant.parse("2026-08-29T00:00:00Z"), ZoneOffset.UTC)

    @Test fun `full names permit letters spaces hyphens and apostrophes only`() {
        assertTrue(AuthValidator.isValidFullName("Juan Dela Cruz"))
        assertTrue(AuthValidator.isValidFullName("Anne-Marie Santos"))
        assertTrue(AuthValidator.isValidFullName("O’Connor"))
        assertFalse(AuthValidator.isValidFullName("Juan123"))
        assertFalse(AuthValidator.isValidFullName("Juan Dela Cruz2"))
    }

    @Test fun `mobile accepts only the two required Philippine formats`() {
        assertTrue(AuthValidator.isValidPhilippineMobile("09171234567"))
        assertTrue(AuthValidator.isValidPhilippineMobile("+639171234567"))
        assertFalse(AuthValidator.isValidPhilippineMobile("0917123456"))
        assertFalse(AuthValidator.isValidPhilippineMobile("091712345678"))
        assertFalse(AuthValidator.isValidPhilippineMobile("+63917123456"))
        assertFalse(AuthValidator.isValidPhilippineMobile("0917ABC4567"))
    }

    @Test fun `age boundary is dynamically calculated`() {
        assertTrue(AuthValidator.isAtLeast18(LocalDate.of(2008, 8, 29), clock))
        assertFalse(AuthValidator.isAtLeast18(LocalDate.of(2008, 8, 30), clock))
    }

    @Test fun `password is strong only when all five rules pass`() {
        val complete = AuthValidator.passwordRequirements("Secure1!")
        assertTrue(complete.isStrong)
        assertTrue(AuthValidator.isStrongPassword("Secure1!"))
        assertFalse(AuthValidator.isStrongPassword("secure1!"))
        assertFalse(AuthValidator.isStrongPassword("SecurePassword!"))
        assertFalse(AuthValidator.isStrongPassword("Secure1"))
        assertFalse(AuthValidator.isStrongPassword("SecurePassword123!TooLong"))
    }
}
