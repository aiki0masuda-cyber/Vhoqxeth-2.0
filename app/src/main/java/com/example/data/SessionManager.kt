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
        prefs.edit().putBoolean(KEY_TERMS_ACCEPTED, accepted).apply()
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
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userProfile.userId.ifBlank { userProfile.customUserId })
            putString(KEY_CUSTOM_USER_ID, userProfile.customUserId)
            putString(KEY_DISPLAY_NAME, userProfile.displayName)
            putString(KEY_PASSWORD_HASH, userProfile.passwordHash)
            putString(KEY_PASSWORD_SALT, userProfile.passwordSalt)
            putString(KEY_FIRST_NAME, userProfile.firstName)
            putString(KEY_LAST_NAME, userProfile.lastName)
            putString(KEY_DOB, userProfile.dateOfBirth)
            putString(KEY_DESCRIPTION, userProfile.description)
            putBoolean(KEY_COMPLETED_OPTIONAL_SETUP, userProfile.hasCompletedOptionalSetup)
            putString(KEY_CURRENT_IP, userProfile.currentIp)
            putStringSet(KEY_ASSOCIATED_IPS, userProfile.associatedIps.toSet())
            putString(KEY_CREATED_AT, userProfile.createdAt)
            apply()
        }
    }

    fun updateCurrentIp(newIp: String) {
        if (newIp.isBlank()) return
        val existingIps = prefs.getStringSet(KEY_ASSOCIATED_IPS, emptySet())?.toMutableSet() ?: mutableSetOf()
        existingIps.add(newIp)

        prefs.edit().apply {
            putString(KEY_CURRENT_IP, newIp)
            putStringSet(KEY_ASSOCIATED_IPS, existingIps)
            apply()
        }
    }

    fun clearSession() {
        val accepted = hasAcceptedTerms()
        prefs.edit().apply {
            clear()
            putBoolean(KEY_TERMS_ACCEPTED, accepted)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "vhoqxeth_session"
        private const val KEY_TERMS_ACCEPTED = "terms_accepted_v1"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_CUSTOM_USER_ID = "custom_user_id"
        private const val KEY_DISPLAY_NAME = "display_name"
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
    }
}
