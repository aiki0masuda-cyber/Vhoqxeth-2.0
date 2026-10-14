package com.example.data

import java.security.MessageDigest
import java.security.SecureRandom

object PasswordSecurityUtil {

    private val secureRandom = SecureRandom()

    /**
     * Generates a 16-byte random cryptographic salt encoded as a hex string.
     */
    fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes a SHA-256 hash of (salt + password).
     */
    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = (salt + password).toByteArray(Charsets.UTF_8)
        val hashBytes = digest.digest(input)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies that the entered password matches the stored salted hash.
     */
    fun verifyPassword(password: String, salt: String, storedHash: String): Boolean {
        if (password.isBlank() || salt.isBlank() || storedHash.isBlank()) {
            return false
        }
        val computed = hashPassword(password, salt)
        return MessageDigest.isEqual(computed.toByteArray(Charsets.UTF_8), storedHash.toByteArray(Charsets.UTF_8))
    }
}
