package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.UserProfile

/**
 * Manages local persistent session state so the login/signup screen only appears on the first run,
 * enabling immediate, zero-latency access to the chat experience on subsequent launches.
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun hasAcceptedTerms(): Boolean {
        return prefs.getBoolean(KEY_TERMS_ACCEPTED, false)
    }

    fun setTermsAccepted(accepted: Boolean = true) {
        prefs.edit().putBoolean(KEY_TERMS_ACCEPTED, accepted).commit()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) &&
                !prefs.getString(KEY_CUSTOM_USER_ID, null).isNullOrBlank()
    }

    fun getSavedUserProfile(): UserProfile? {
        if (!isLoggedIn()) return null
        val customUserId = prefs.getString(KEY_CUSTOM_USER_ID, "") ?: ""
        val displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: customUserId
        val userId = prefs.getString(KEY_USER_ID, "") ?: customUserId
        val role = prefs.getString(KEY_ROLE, UserProfile.ROLE_STUDENT) ?: UserProfile.ROLE_STUDENT
        val grade = prefs.getString(KEY_GRADE, "") ?: ""
        val section = prefs.getString(KEY_SECTION, "") ?: ""
        val subject = prefs.getString(KEY_SUBJECT, "") ?: ""
        val department = prefs.getString(KEY_DEPARTMENT, "") ?: ""
        val employeeId = prefs.getString(KEY_EMPLOYEE_ID, "") ?: ""
        val designation = prefs.getString(KEY_DESIGNATION, "") ?: ""
        val passwordHash = prefs.getString(KEY_PASSWORD_HASH, "") ?: ""
        val passwordSalt = prefs.getString(KEY_PASSWORD_SALT, "") ?: ""
        val firstName = prefs.getString(KEY_FIRST_NAME, "") ?: ""
        val lastName = prefs.getString(KEY_LAST_NAME, "") ?: ""
        val dateOfBirth = prefs.getString(KEY_DOB, "") ?: ""
        val description = prefs.getString(KEY_DESCRIPTION, "") ?: ""
        val hasCompletedOptionalSetup = prefs.getBoolean(KEY_COMPLETED_OPTIONAL_SETUP, false)
        val currentIp = prefs.getString(KEY_CURRENT_IP, "") ?: ""
        val associatedIpsSet = prefs.getStringSet(KEY_ASSOCIATED_IPS, emptySet()) ?: emptySet()
        val createdAt = prefs.getString(KEY_CREATED_AT, "") ?: ""

        return UserProfile(
            userId = userId,
            displayName = displayName,
            customUserId = customUserId,
            role = role,
            grade = grade,
            section = section,
            subject = subject,
            department = department,
            employeeId = employeeId,
            designation = designation,
            passwordHash = passwordHash,
            passwordSalt = passwordSalt,
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            description = description,
            hasCompletedOptionalSetup = hasCompletedOptionalSetup,
            associatedIps = associatedIpsSet.toList(),
            currentIp = currentIp,
            createdAt = createdAt
        )
    }

    fun saveSession(userProfile: UserProfile) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USER_ID, userProfile.userId.ifBlank { userProfile.customUserId })
        editor.putString(KEY_CUSTOM_USER_ID, userProfile.customUserId)
        editor.putString(KEY_DISPLAY_NAME, userProfile.displayName)
        editor.putString(KEY_ROLE, userProfile.role)
        editor.putString(KEY_GRADE, userProfile.grade)
        editor.putString(KEY_SECTION, userProfile.section)
        editor.putString(KEY_SUBJECT, userProfile.subject)
        editor.putString(KEY_DEPARTMENT, userProfile.department)
        editor.putString(KEY_EMPLOYEE_ID, userProfile.employeeId)
        editor.putString(KEY_DESIGNATION, userProfile.designation)
        editor.putString(KEY_PASSWORD_HASH, userProfile.passwordHash)
        editor.putString(KEY_PASSWORD_SALT, userProfile.passwordSalt)
        editor.putString(KEY_FIRST_NAME, userProfile.firstName)
        editor.putString(KEY_LAST_NAME, userProfile.lastName)
        editor.putString(KEY_DOB, userProfile.dateOfBirth)
        editor.putString(KEY_DESCRIPTION, userProfile.description)
        editor.putBoolean(KEY_COMPLETED_OPTIONAL_SETUP, userProfile.hasCompletedOptionalSetup)
        editor.putString(KEY_CURRENT_IP, userProfile.currentIp)
        editor.putStringSet(KEY_ASSOCIATED_IPS, userProfile.associatedIps.toSet())
        editor.putString(KEY_CREATED_AT, userProfile.createdAt)
        editor.commit()
    }

    fun updateCurrentIp(newIp: String) {
        if (newIp.isBlank()) return
        val existingIps = prefs.getStringSet(KEY_ASSOCIATED_IPS, emptySet())?.toMutableSet() ?: mutableSetOf()
        existingIps.add(newIp)

        prefs.edit()
            .putString(KEY_CURRENT_IP, newIp)
            .putStringSet(KEY_ASSOCIATED_IPS, existingIps)
            .commit()
    }

    fun getCloudflareWorkerUrl(): String {
        return prefs.getString(KEY_CLOUDFLARE_WORKER_URL, "https://b2-chat-gatekeeper.aiki0masuda.workers.dev/")
            ?.ifBlank { "https://b2-chat-gatekeeper.aiki0masuda.workers.dev/" }
            ?: "https://b2-chat-gatekeeper.aiki0masuda.workers.dev/"
    }

    fun setCloudflareWorkerUrl(url: String) {
        prefs.edit().putString(KEY_CLOUDFLARE_WORKER_URL, url.trim()).commit()
    }

    fun getHuggingFaceApiKey(): String {
        val saved = prefs.getString(KEY_HUGGINGFACE_API_KEY, "") ?: ""
        if (saved.isNotBlank()) return saved
        return try {
            val field = com.example.BuildConfig::class.java.getField("HUGGINGFACE_API_KEY")
            val key = (field.get(null) as? String)?.trim().orEmpty()
            if (key == "hf_placeholder_key" || key == "MY_NEW_API_KEY_DEFAULT_VALUE") "" else key
        } catch (_: Exception) {
            ""
        }
    }

    fun setHuggingFaceApiKey(apiKey: String) {
        prefs.edit().putString(KEY_HUGGINGFACE_API_KEY, apiKey.trim()).commit()
    }

    fun getHuggingFaceModel(): String {
        return prefs.getString(KEY_HUGGINGFACE_MODEL, "black-forest-labs/FLUX.1-schnell")
            ?.ifBlank { "black-forest-labs/FLUX.1-schnell" }
            ?: "black-forest-labs/FLUX.1-schnell"
    }

    fun setHuggingFaceModel(modelId: String) {
        prefs.edit().putString(KEY_HUGGINGFACE_MODEL, modelId.trim()).commit()
    }

    fun getHuggingFaceCustomModel(): String {
        return prefs.getString(KEY_HUGGINGFACE_CUSTOM_MODEL, "") ?: ""
    }

    fun setHuggingFaceCustomModel(customModelId: String) {
        prefs.edit().putString(KEY_HUGGINGFACE_CUSTOM_MODEL, customModelId.trim()).commit()
    }

    fun isLowTokenModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOW_TOKEN_MODE, true)
    }

    fun setLowTokenModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOW_TOKEN_MODE, enabled).commit()
    }

    fun recordDiagnosticLog(tag: String, message: String) {
        val currentLogs = prefs.getString(KEY_ADMIN_DIAGNOSTIC_LOGS, "") ?: ""
        val newEntry = "[${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}] $tag: $message\n"
        val trimmed = (newEntry + currentLogs).take(3000)
        prefs.edit().putString(KEY_ADMIN_DIAGNOSTIC_LOGS, trimmed).commit()
    }

    fun getDiagnosticLogs(): String {
        return prefs.getString(KEY_ADMIN_DIAGNOSTIC_LOGS, "") ?: ""
    }

    fun clearSession() {
        val accepted = hasAcceptedTerms()
        prefs.edit()
            .clear()
            .putBoolean(KEY_TERMS_ACCEPTED, accepted)
            .commit()
    }

    companion object {
        private const val PREFS_NAME = "vhoqxeth_session"
        private const val KEY_TERMS_ACCEPTED = "terms_accepted_v1"
        private const val KEY_HUGGINGFACE_API_KEY = "huggingface_api_key"
        private const val KEY_HUGGINGFACE_MODEL = "huggingface_model"
        private const val KEY_HUGGINGFACE_CUSTOM_MODEL = "huggingface_custom_model"
        private const val KEY_LOW_TOKEN_MODE = "hf_low_token_mode"
        private const val KEY_ADMIN_DIAGNOSTIC_LOGS = "admin_diagnostic_logs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_CUSTOM_USER_ID = "custom_user_id"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ROLE = "role"
        private const val KEY_GRADE = "grade"
        private const val KEY_SECTION = "section"
        private const val KEY_SUBJECT = "subject"
        private const val KEY_DEPARTMENT = "department"
        private const val KEY_EMPLOYEE_ID = "employee_id"
        private const val KEY_DESIGNATION = "designation"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_PASSWORD_SALT = "password_salt"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_DOB = "date_of_birth"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_COMPLETED_OPTIONAL_SETUP = "has_completed_optional_setup"
        private const val KEY_CURRENT_IP = "current_ip"
        private const val KEY_ASSOCIATED_IPS = "associated_ips"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_CLOUDFLARE_WORKER_URL = "cloudflare_worker_url"
    }
}
