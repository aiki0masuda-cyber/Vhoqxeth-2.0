package com.example.model

/**
 * Entry stored in /registered_ips/{ipKey} for O(1) lookup of device network IP ownership.
 * All properties provide default values to support Firestore no-arg deserialization.
 */
data class RegisteredIpEntry(
    val ip: String = "",
    val userId: String = "",
    val customUserId: String = "",
    val displayName: String = "",
    val registeredAt: String = ""
)
