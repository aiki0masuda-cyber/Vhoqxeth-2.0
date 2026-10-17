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

class AdminRepository(
    private val db: FirebaseFirestore
) {
    private val tag = "AdminRepository"

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun getCurrentIsoTimestamp(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return format.format(Date())
    }

    suspend fun getAllUsers(): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("users").get().await()
            val users = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Log.e(tag, "Failed to get users: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun createUser(
        displayName: String,
        customUserId: String,
        initialIp: String,
        role: String = UserProfile.ROLE_STUDENT,
        grade: String = "",
        section: String = "",
        subject: String = "",
        department: String = "",
        employeeId: String = "",
        designation: String = ""
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val handle = customUserId.trim().removePrefix("@")
            if (handle.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Custom User ID cannot be empty."))
            }

            val existing = db.collection("users").document(handle).get().await()
            if (existing.exists()) {
                return@withContext Result.failure(IllegalArgumentException("User ID '@$handle' is already taken."))
            }

            val now = getCurrentIsoTimestamp()
            val ipList = if (initialIp.isNotBlank()) listOf(initialIp.trim()) else emptyList()
            val newProfile = UserProfile(
                userId = handle,
                displayName = displayName.trim().ifBlank { handle },
                customUserId = handle,
                role = if (role.equals(UserProfile.ROLE_TEACHER, ignoreCase = true)) UserProfile.ROLE_TEACHER else UserProfile.ROLE_STUDENT,
                grade = grade.trim(),
                section = section.trim(),
                subject = subject.trim(),
                department = department.trim(),
                employeeId = employeeId.trim(),
                designation = designation.trim(),
                associatedIps = ipList,
                currentIp = initialIp.trim(),
                createdAt = now
            )

            // Write user doc
            db.collection("users").document(handle).set(
                mapOf(
                    "userId" to handle,
                    "displayName" to newProfile.displayName,
                    "customUserId" to handle,
                    "role" to newProfile.role,
                    "grade" to newProfile.grade,
                    "section" to newProfile.section,
                    "subject" to newProfile.subject,
                    "department" to newProfile.department,
                    "employeeId" to newProfile.employeeId,
                    "designation" to newProfile.designation,
                    "associatedIps" to ipList,
                    "currentIp" to newProfile.currentIp,
                    "createdAt" to now
                )
            ).await()

            // Write IP mapping if provided
            if (initialIp.isNotBlank()) {
                val ipKey = DeviceIpManager.sanitizeIpKey(initialIp)
                db.collection("registered_ips").document(ipKey).set(
                    mapOf(
                        "ip" to initialIp.trim(),
                        "userId" to handle,
                        "customUserId" to handle,
                        "displayName" to newProfile.displayName,
                        "registeredAt" to now
                    )
                ).await()
            }

            Result.success(newProfile)
        } catch (e: Exception) {
            Log.e(tag, "Failed to create user: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateUser(
        originalHandle: String,
        updatedProfile: UserProfile
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val original = originalHandle.trim().removePrefix("@")
            val newHandle = updatedProfile.customUserId.trim().removePrefix("@")

            val userPayload = mapOf(
                "userId" to newHandle,
                "displayName" to updatedProfile.displayName.trim().ifBlank { newHandle },
                "customUserId" to newHandle,
                "role" to updatedProfile.role,
                "grade" to updatedProfile.grade,
                "section" to updatedProfile.section,
                "subject" to updatedProfile.subject,
                "department" to updatedProfile.department,
                "employeeId" to updatedProfile.employeeId,
                "designation" to updatedProfile.designation,
                "passwordHash" to updatedProfile.passwordHash,
                "passwordSalt" to updatedProfile.passwordSalt,
                "firstName" to updatedProfile.firstName,
                "lastName" to updatedProfile.lastName,
                "dateOfBirth" to updatedProfile.dateOfBirth,
                "description" to updatedProfile.description,
                "hasCompletedOptionalSetup" to updatedProfile.hasCompletedOptionalSetup,
                "associatedIps" to updatedProfile.associatedIps,
                "currentIp" to updatedProfile.currentIp,
                "createdAt" to updatedProfile.createdAt.ifBlank { getCurrentIsoTimestamp() }
            )

            // Save new/updated document
            db.collection("users").document(newHandle).set(userPayload).await()

            // If handle changed, delete the old document
            if (original != newHandle && original.isNotBlank()) {
                db.collection("users").document(original).delete().await()
            }

            // Sync registered_ips for all associated IPs
            val now = getCurrentIsoTimestamp()
            for (ip in updatedProfile.associatedIps) {
                val ipKey = DeviceIpManager.sanitizeIpKey(ip)
                db.collection("registered_ips").document(ipKey).set(
                    mapOf(
                        "ip" to ip,
                        "userId" to newHandle,
                        "customUserId" to newHandle,
                        "displayName" to updatedProfile.displayName,
                        "registeredAt" to now
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update user: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUser(
        customUserId: String,
        associatedIps: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val handle = customUserId.trim().removePrefix("@")
            // 1. Delete user profile document
            db.collection("users").document(handle).delete().await()

            // 2. Unlink & delete all associated IP documents
            for (ip in associatedIps) {
                if (ip.isNotBlank()) {
                    val ipKey = DeviceIpManager.sanitizeIpKey(ip)
                    try {
                        db.collection("registered_ips").document(ipKey).delete().await()
                    } catch (e: Exception) {
                        Log.w(tag, "Could not delete IP key $ipKey: ${e.message}")
                    }
                }
            }

            Log.i(tag, "Successfully deleted user @$handle and unlinked ${associatedIps.size} IPs")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete user: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllRegisteredIps(): Result<List<RegisteredIpEntry>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("registered_ips").get().await()
            val entries = snapshot.documents.mapNotNull { it.toObject(RegisteredIpEntry::class.java) }
            Result.success(entries)
        } catch (e: Exception) {
            Log.e(tag, "Failed to get registered IPs: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun addRegisteredIp(
        ip: String,
        customUserId: String,
        displayName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanIp = ip.trim()
            val handle = customUserId.trim().removePrefix("@")
            val ipKey = DeviceIpManager.sanitizeIpKey(cleanIp)
            val now = getCurrentIsoTimestamp()

            db.collection("registered_ips").document(ipKey).set(
                mapOf(
                    "ip" to cleanIp,
                    "userId" to handle,
                    "customUserId" to handle,
                    "displayName" to displayName.trim(),
                    "registeredAt" to now
                )
            ).await()

            // Add IP to user's associatedIps if user exists
            val userDoc = db.collection("users").document(handle).get().await()
            if (userDoc.exists()) {
                val profile = userDoc.toObject(UserProfile::class.java)
                if (profile != null && !profile.associatedIps.contains(cleanIp)) {
                    val updatedList = profile.associatedIps + cleanIp
                    db.collection("users").document(handle).update(
                        mapOf(
                            "associatedIps" to updatedList,
                            "currentIp" to cleanIp
                        )
                    ).await()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to add registered IP: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteRegisteredIp(
        ip: String,
        customUserId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ipKey = DeviceIpManager.sanitizeIpKey(ip)
            db.collection("registered_ips").document(ipKey).delete().await()

            // If user handle is provided, remove this IP from their associatedIps
            if (!customUserId.isNullOrBlank()) {
                val handle = customUserId.trim().removePrefix("@")
                val userDoc = db.collection("users").document(handle).get().await()
                if (userDoc.exists()) {
                    val profile = userDoc.toObject(UserProfile::class.java)
                    if (profile != null) {
                        val updatedList = profile.associatedIps.filterNot { it == ip }
                        db.collection("users").document(handle).update(
                            mapOf("associatedIps" to updatedList)
                        ).await()
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete registered IP: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getConnectionProbes(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("connection_probes").get().await()
            val list = snapshot.documents.map { doc ->
                val map = doc.data?.toMutableMap() ?: mutableMapOf<String, Any>()
                map["_id"] = doc.id
                map
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(tag, "Failed to get connection probes: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun createTestProbe(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = getCurrentIsoTimestamp()
            val probeId = "admin_probe_${System.currentTimeMillis()}"
            db.collection("connection_probes").document(probeId).set(
                mapOf(
                    "status" to "ACTIVE_ADMIN_PROBE",
                    "databaseId" to "dav-infoexchange-main",
                    "clientApp" to "DAV InfoExchange Master Console",
                    "timestamp" to now
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to create probe: ${e.message}", e)
            Result.failure(e)
        }
    }
}
