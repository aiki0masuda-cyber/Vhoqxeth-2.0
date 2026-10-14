package com.example.model

/**
 * User profile entity stored in Firestore under /users/{userId}.
 * All properties provide default values to support Firestore no-arg deserialization.
 */
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val customUserId: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val description: String = "",
    val hasCompletedOptionalSetup: Boolean = false,
    val associatedIps: List<String> = emptyList(),
    val currentIp: String = "",
    val createdAt: String = ""
)
