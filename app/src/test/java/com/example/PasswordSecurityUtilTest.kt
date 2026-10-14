package com.example

import com.example.data.PasswordSecurityUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordSecurityUtilTest {

    @Test
    fun generateSalt_producesDistinctHexStringsOfExpectedLength() {
        val salt1 = PasswordSecurityUtil.generateSalt()
        val salt2 = PasswordSecurityUtil.generateSalt()

        assertNotNull(salt1)
        assertNotNull(salt2)
        assertEquals(32, salt1.length) // 16 bytes = 32 hex chars
        assertEquals(32, salt2.length)
        assertNotEquals(salt1, salt2)
    }

    @Test
    fun hashPassword_deterministicForSameSaltAndPassword() {
        val password = "secret_mesh_key_123"
        val salt = "abcdef0123456789abcdef0123456789"

        val hash1 = PasswordSecurityUtil.hashPassword(password, salt)
        val hash2 = PasswordSecurityUtil.hashPassword(password, salt)

        assertEquals(64, hash1.length) // SHA-256 = 32 bytes = 64 hex chars
        assertEquals(hash1, hash2)
    }

    @Test
    fun hashPassword_differentSaltProducesDifferentHash() {
        val password = "secret_mesh_key_123"
        val salt1 = PasswordSecurityUtil.generateSalt()
        val salt2 = PasswordSecurityUtil.generateSalt()

        val hash1 = PasswordSecurityUtil.hashPassword(password, salt1)
        val hash2 = PasswordSecurityUtil.hashPassword(password, salt2)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun verifyPassword_succeedsForCorrectPassword() {
        val password = "correct_horse_battery_staple"
        val salt = PasswordSecurityUtil.generateSalt()
        val storedHash = PasswordSecurityUtil.hashPassword(password, salt)

        val isValid = PasswordSecurityUtil.verifyPassword(password, salt, storedHash)
        assertTrue("Verification should succeed for correct password", isValid)
    }

    @Test
    fun verifyPassword_failsForIncorrectPassword() {
        val password = "correct_password"
        val wrongPassword = "wrong_password"
        val salt = PasswordSecurityUtil.generateSalt()
        val storedHash = PasswordSecurityUtil.hashPassword(password, salt)

        val isValid = PasswordSecurityUtil.verifyPassword(wrongPassword, salt, storedHash)
        assertFalse("Verification must fail for wrong password", isValid)
    }

    @Test
    fun verifyPassword_failsForBlankOrEmptyInputs() {
        val salt = PasswordSecurityUtil.generateSalt()
        val hash = PasswordSecurityUtil.hashPassword("pass", salt)

        assertFalse(PasswordSecurityUtil.verifyPassword("", salt, hash))
        assertFalse(PasswordSecurityUtil.verifyPassword("pass", "", hash))
        assertFalse(PasswordSecurityUtil.verifyPassword("pass", salt, ""))
    }
}
