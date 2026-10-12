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

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) &&
                !prefs.getString(KEY_CUSTOM_USER_ID, null).isNullOrBlank()
    }

    fun getSavedUserProfile(): UserProfile? {
        if (!isLoggedIn()) return null
        val customUserId = prefs.getString(KEY_CUSTOM_USER_ID, "") ?: ""
        val displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: customUserId
        val userId = prefs.getString(KEY_USER_ID, "") ?: customUserId
        val currentIp = prefs.getString(KEY_CURRENT_IP, "") ?: ""
        val associatedIpsSet = prefs.getStringSet(KEY_ASSOCIATED_IPS, emptySet()) ?: emptySet()
        val createdAt = prefs.getString(KEY_CREATED_AT, "") ?: ""

        return UserProfile(
            userId = userId,
            displayName = displayName,
            customUserId = customUserId,
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
        prefs.edit().apply {
            clear()
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "vhoqxeth_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_CUSTOM_USER_ID = "custom_user_id"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_CURRENT_IP = "current_ip"
        private const val KEY_ASSOCIATED_IPS = "associated_ips"
        private const val KEY_CREATED_AT = "created_at"
    }
}
