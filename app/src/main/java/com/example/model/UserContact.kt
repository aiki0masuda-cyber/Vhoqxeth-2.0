package com.example.model

/**
 * Contact entry saved under /users/{userId}/contacts/{contactId}.
 * All properties provide default values for Firestore deserialization.
 */
data class UserContact(
    val contactUserId: String = "",
    val displayName: String = "",
    val currentIp: String = "",
    val addedAt: String = "",
    val avatarColorStart: Long = 0xFF4F46E5,
    val avatarColorEnd: Long = 0xFF2563EB
) {
    val initials: String
        get() = displayName.trim().split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifBlank { contactUserId.take(2).uppercase().ifBlank { "VQ" } }
}
