package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.RegisteredIpEntry
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuthRepository(
    private val db: FirebaseFirestore
) {
    private val tag = "AuthRepository"

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun getCurrentIsoTimestamp(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return format.format(Date())
    }

    /**
     * Checks if a detected device IP has been registered in the database.
     */
    suspend fun checkIpRegistration(ip: String): Result<RegisteredIpEntry?> = withContext(Dispatchers.IO) {
        try {
            val key = DeviceIpManager.sanitizeIpKey(ip)
            val doc = db.collection("registered_ips").document(key).get().await()
            if (doc.exists()) {
                val entry = doc.toObject(RegisteredIpEntry::class.java)
                Log.i(tag, "Found registered IP entry for $ip -> @${entry?.customUserId}")
                Result.success(entry)
            } else {
                Log.i(tag, "IP $ip is not registered in database")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to check IP registration: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches the user profile for a given user ID / custom handle.
     */
    suspend fun getUserProfile(userId: String): Result<UserProfile?> = withContext(Dispatchers.IO) {
        try {
            val key = userId.trim().removePrefix("@")
            val doc = db.collection("users").document(key).get().await()
            if (doc.exists()) {
                val profile = doc.toObject(UserProfile::class.java)
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch user profile for $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Registers a new user profile using only their chosen display name, custom user ID handle,
     * and detected IP address, writing directly to Firestore without third-party auth gates.
     */
    suspend fun registerUserProfile(
        displayName: String,
        customUserId: String,
        ip: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val now = getCurrentIsoTimestamp()
            val sanitizedHandle = customUserId.trim().removePrefix("@")

            if (sanitizedHandle.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Custom User ID cannot be empty."))
            }

            // Check if handle already exists
            val existingDoc = db.collection("users").document(sanitizedHandle).get().await()
            if (existingDoc.exists()) {
                return@withContext Result.failure(
                    IllegalArgumentException("User ID '@$sanitizedHandle' is already taken. Please choose another ID or log in.")
                )
            }

            val newProfile = UserProfile(
                userId = sanitizedHandle,
                displayName = displayName.trim().ifBlank { sanitizedHandle },
                customUserId = sanitizedHandle,
                associatedIps = listOf(ip),
                currentIp = ip,
                createdAt = now
            )

            // 1. Write user profile
            val userPayload = mapOf(
                "userId" to sanitizedHandle,
                "displayName" to newProfile.displayName,
                "customUserId" to sanitizedHandle,
                "associatedIps" to listOf(ip),
                "currentIp" to ip,
                "createdAt" to now
            )
            db.collection("users").document(sanitizedHandle).set(userPayload).await()

            // 2. Write IP index entry
            val ipKey = DeviceIpManager.sanitizeIpKey(ip)
            val ipPayload = mapOf(
                "ip" to ip,
                "userId" to sanitizedHandle,
                "customUserId" to sanitizedHandle,
                "displayName" to newProfile.displayName,
                "registeredAt" to now
            )
            db.collection("registered_ips").document(ipKey).set(ipPayload).await()

            Log.i(tag, "Successfully registered user @$sanitizedHandle on IP $ip")
            Result.success(newProfile)
        } catch (e: Exception) {
            Log.e(tag, "Failed to register user profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Links a new device IP to an existing custom user ID handle.
     */
    suspend fun linkIpToUserProfile(
        customUserId: String,
        ip: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val sanitizedHandle = customUserId.trim().removePrefix("@")
            val userDoc = db.collection("users").document(sanitizedHandle).get().await()
            val existingProfile = userDoc.toObject(UserProfile::class.java)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("User '@$sanitizedHandle' was not found. Please verify the ID or create an account.")
                )

            val currentList = existingProfile.associatedIps.toMutableList()
            if (!currentList.contains(ip)) {
                currentList.add(ip)
            }

            val updatedProfile = existingProfile.copy(
                associatedIps = currentList,
                currentIp = ip
            )

            val now = getCurrentIsoTimestamp()
            val userPayload = mapOf(
                "userId" to sanitizedHandle,
                "displayName" to updatedProfile.displayName,
                "customUserId" to updatedProfile.customUserId,
                "associatedIps" to currentList,
                "currentIp" to ip,
                "createdAt" to updatedProfile.createdAt
            )
            db.collection("users").document(sanitizedHandle).set(userPayload).await()

            // Map IP key to user
            val ipKey = DeviceIpManager.sanitizeIpKey(ip)
            val ipPayload = mapOf(
                "ip" to ip,
                "userId" to sanitizedHandle,
                "customUserId" to updatedProfile.customUserId,
                "displayName" to updatedProfile.displayName,
                "registeredAt" to now
            )
            db.collection("registered_ips").document(ipKey).set(ipPayload).await()

            Log.i(tag, "Successfully linked IP $ip to user @${updatedProfile.customUserId}")
            Result.success(updatedProfile)
        } catch (e: Exception) {
            Log.e(tag, "Failed to link IP to user profile: ${e.message}", e)
            Result.failure(e)
        }
    }
}
