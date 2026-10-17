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
     * Registers a new user profile with required Visible Name, Handle/User ID,
     * and a salted SHA-256 password hash.
     */
    suspend fun registerUserProfile(
        displayName: String,
        customUserId: String,
        password: String,
        ip: String,
        role: String = UserProfile.ROLE_STUDENT,
        grade: String = "",
        section: String = "",
        subject: String = "",
        department: String = "",
        employeeId: String = "",
        designation: String = ""
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val now = getCurrentIsoTimestamp()
            val sanitizedHandle = customUserId.trim().removePrefix("@")

            if (sanitizedHandle.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("User ID / Handle cannot be empty."))
            }

            if (password.length < 4) {
                return@withContext Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
            }

            // Check if handle already exists
            val existingDoc = db.collection("users").document(sanitizedHandle).get().await()
            if (existingDoc.exists()) {
                return@withContext Result.failure(
                    IllegalArgumentException("User ID '@$sanitizedHandle' is already taken. Please choose another ID or log in.")
                )
            }

            val salt = PasswordSecurityUtil.generateSalt()
            val hash = PasswordSecurityUtil.hashPassword(password, salt)

            val newProfile = UserProfile(
                userId = sanitizedHandle,
                displayName = displayName.trim().ifBlank { sanitizedHandle },
                customUserId = sanitizedHandle,
                passwordHash = hash,
                passwordSalt = salt,
                role = if (role.equals(UserProfile.ROLE_TEACHER, ignoreCase = true)) UserProfile.ROLE_TEACHER else UserProfile.ROLE_STUDENT,
                grade = grade.trim(),
                section = section.trim(),
                subject = subject.trim(),
                department = department.trim(),
                employeeId = employeeId.trim(),
                designation = designation.trim(),
                firstName = "",
                lastName = "",
                dateOfBirth = "",
                description = "",
                hasCompletedOptionalSetup = false,
                associatedIps = if (ip.isNotBlank()) listOf(ip) else emptyList(),
                currentIp = ip,
                createdAt = now
            )

            // 1. Write user profile
            val userPayload = mapOf(
                "userId" to sanitizedHandle,
                "displayName" to newProfile.displayName,
                "customUserId" to sanitizedHandle,
                "passwordHash" to hash,
                "passwordSalt" to salt,
                "role" to newProfile.role,
                "grade" to newProfile.grade,
                "section" to newProfile.section,
                "subject" to newProfile.subject,
                "department" to newProfile.department,
                "employeeId" to newProfile.employeeId,
                "designation" to newProfile.designation,
                "firstName" to "",
                "lastName" to "",
                "dateOfBirth" to "",
                "description" to "",
                "hasCompletedOptionalSetup" to false,
                "associatedIps" to newProfile.associatedIps,
                "currentIp" to ip,
                "createdAt" to now
            )
            db.collection("users").document(sanitizedHandle).set(userPayload).await()

            // 2. Write IP index entry if IP is valid
            if (ip.isNotBlank()) {
                val ipKey = DeviceIpManager.sanitizeIpKey(ip)
                val ipPayload = mapOf(
                    "ip" to ip,
                    "userId" to sanitizedHandle,
                    "customUserId" to sanitizedHandle,
                    "displayName" to newProfile.displayName,
                    "registeredAt" to now
                )
                db.collection("registered_ips").document(ipKey).set(ipPayload).await()
            }

            Log.i(tag, "Successfully registered user @$sanitizedHandle with salted password on IP $ip")
            Result.success(newProfile)
        } catch (e: Exception) {
            Log.e(tag, "Failed to register user profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Authenticates a user by validating their handle and password against the stored salted hash.
     */
    suspend fun authenticateWithPassword(
        customUserId: String,
        password: String,
        currentIp: String = ""
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val sanitizedHandle = customUserId.trim().removePrefix("@")
            if (sanitizedHandle.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Please enter your User ID."))
            }

            val userDoc = db.collection("users").document(sanitizedHandle).get().await()
            if (!userDoc.exists()) {
                return@withContext Result.failure(
                    IllegalArgumentException("User account '@$sanitizedHandle' was not found.")
                )
            }

            val existingProfile = userDoc.toObject(UserProfile::class.java)
                ?: return@withContext Result.failure(IllegalArgumentException("Failed to read user account profile."))

            // Validate password if user has password set
            if (existingProfile.passwordHash.isNotBlank()) {
                val isValid = PasswordSecurityUtil.verifyPassword(
                    password = password,
                    salt = existingProfile.passwordSalt,
                    storedHash = existingProfile.passwordHash
                )
                if (!isValid) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Incorrect password. Please verify your credentials.")
                    )
                }
            }

            // Sync current IP if changed
            if (currentIp.isNotBlank() && currentIp != existingProfile.currentIp) {
                linkIpToUserProfile(sanitizedHandle, currentIp)
            }

            Log.i(tag, "User @$sanitizedHandle authenticated successfully")
            Result.success(existingProfile)
        } catch (e: Exception) {
            Log.e(tag, "Authentication failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Updates optional profile details (first name, last name, DOB, description).
     */
    suspend fun updateOptionalProfileDetails(
        userId: String,
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        description: String,
        role: String? = null,
        grade: String? = null,
        section: String? = null,
        subject: String? = null,
        department: String? = null,
        employeeId: String? = null,
        designation: String? = null
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val key = userId.trim().removePrefix("@")
            val userDoc = db.collection("users").document(key).get().await()
            val existing = userDoc.toObject(UserProfile::class.java)
                ?: return@withContext Result.failure(IllegalArgumentException("User profile not found."))

            val updatedRole = role?.trim() ?: existing.role
            val updatedGrade = grade?.trim() ?: existing.grade
            val updatedSection = section?.trim() ?: existing.section
            val updatedSubject = subject?.trim() ?: existing.subject
            val updatedDept = department?.trim() ?: existing.department
            val updatedEmpId = employeeId?.trim() ?: existing.employeeId
            val updatedDesig = designation?.trim() ?: existing.designation

            val updated = existing.copy(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                dateOfBirth = dateOfBirth.trim(),
                description = description.trim(),
                role = updatedRole,
                grade = updatedGrade,
                section = updatedSection,
                subject = updatedSubject,
                department = updatedDept,
                employeeId = updatedEmpId,
                designation = updatedDesig,
                hasCompletedOptionalSetup = true
            )

            val updatePayload = mapOf<String, Any>(
                "firstName" to updated.firstName,
                "lastName" to updated.lastName,
                "dateOfBirth" to updated.dateOfBirth,
                "description" to updated.description,
                "role" to updated.role,
                "grade" to updated.grade,
                "section" to updated.section,
                "subject" to updated.subject,
                "department" to updated.department,
                "employeeId" to updated.employeeId,
                "designation" to updated.designation,
                "hasCompletedOptionalSetup" to true
            )
            db.collection("users").document(key).update(updatePayload).await()

            Log.i(tag, "Updated optional profile details for @$key")
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update profile details: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Marks optional setup as completed/skipped so modal does not re-appear on subsequent boots.
     */
    suspend fun skipOptionalProfileDetails(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val key = userId.trim().removePrefix("@")
            db.collection("users").document(key).update("hasCompletedOptionalSetup", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to mark optional details as skipped: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Updates the user's visible display name across their profile and IP mapping.
     */
    suspend fun updateVisibleName(userId: String, newDisplayName: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val key = userId.trim().removePrefix("@")
            val trimmedName = newDisplayName.trim()
            if (trimmedName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Visible name cannot be blank."))
            }

            val userDoc = db.collection("users").document(key).get().await()
            val existing = userDoc.toObject(UserProfile::class.java)
                ?: return@withContext Result.failure(IllegalArgumentException("User profile not found."))

            val updated = existing.copy(displayName = trimmedName)
            db.collection("users").document(key).update("displayName", trimmedName).await()

            if (existing.currentIp.isNotBlank()) {
                val ipKey = DeviceIpManager.sanitizeIpKey(existing.currentIp)
                db.collection("registered_ips").document(ipKey).update("displayName", trimmedName).await()
            }

            Log.i(tag, "Updated visible name for @$key to '$trimmedName'")
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update visible name: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Changes user password after verifying current password.
     */
    suspend fun changePassword(
        userId: String,
        currentPassword: String,
        newPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val key = userId.trim().removePrefix("@")
            if (newPassword.length < 4) {
                return@withContext Result.failure(IllegalArgumentException("New password must be at least 4 characters."))
            }

            val userDoc = db.collection("users").document(key).get().await()
            val existing = userDoc.toObject(UserProfile::class.java)
                ?: return@withContext Result.failure(IllegalArgumentException("User profile not found."))

            if (existing.passwordHash.isNotBlank()) {
                val isOldValid = PasswordSecurityUtil.verifyPassword(
                    password = currentPassword,
                    salt = existing.passwordSalt,
                    storedHash = existing.passwordHash
                )
                if (!isOldValid) {
                    return@withContext Result.failure(IllegalArgumentException("Current password does not match."))
                }
            }

            val newSalt = PasswordSecurityUtil.generateSalt()
            val newHash = PasswordSecurityUtil.hashPassword(newPassword, newSalt)

            val updatePayload = mapOf<String, Any>(
                "passwordHash" to newHash,
                "passwordSalt" to newSalt
            )
            db.collection("users").document(key).update(updatePayload).await()

            Log.i(tag, "Successfully updated password for @$key")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to change password: ${e.message}", e)
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
                "passwordHash" to updatedProfile.passwordHash,
                "passwordSalt" to updatedProfile.passwordSalt,
                "firstName" to updatedProfile.firstName,
                "lastName" to updatedProfile.lastName,
                "dateOfBirth" to updatedProfile.dateOfBirth,
                "description" to updatedProfile.description,
                "hasCompletedOptionalSetup" to updatedProfile.hasCompletedOptionalSetup,
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
