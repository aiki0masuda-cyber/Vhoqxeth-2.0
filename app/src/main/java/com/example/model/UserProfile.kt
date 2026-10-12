package com.example.model

/**
 * User profile entity stored in Firestore under /users/{userId}.
 * All properties provide default values to support Firestore no-arg deserialization.
 */
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val customUserId: String = "",
    val associatedIps: List<String> = emptyList(),
    val currentIp: String = "",
    val createdAt: String = ""
)
