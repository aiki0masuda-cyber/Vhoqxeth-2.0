package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.SessionManager
import com.example.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SessionManagerTest {

    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        sessionManager = SessionManager(ApplicationProvider.getApplicationContext())
        sessionManager.clearSession()
    }

    @Test
    fun defaultStateIsNotLoggedIn() {
        assertFalse(sessionManager.isLoggedIn())
        assertNull(sessionManager.getSavedUserProfile())
    }

    @Test
    fun saveSessionPersistsUserProfileAndLoggedInState() {
        val profile = UserProfile(
            userId = "cipher_fox",
            displayName = "Alex Hunter",
            customUserId = "cipher_fox",
            associatedIps = listOf("198.51.100.24"),
            currentIp = "198.51.100.24",
            createdAt = "2026-10-09T15:00:00Z"
        )

        sessionManager.saveSession(profile)

        assertTrue(sessionManager.isLoggedIn())
        val saved = sessionManager.getSavedUserProfile()
        assertNotNull(saved)
        assertEquals("cipher_fox", saved?.customUserId)
        assertEquals("Alex Hunter", saved?.displayName)
        assertEquals("198.51.100.24", saved?.currentIp)
        assertTrue(saved?.associatedIps?.contains("198.51.100.24") == true)
    }

    @Test
    fun updateCurrentIpAddsNewIpToAssociatedList() {
        val profile = UserProfile(
            userId = "cipher_fox",
            displayName = "Alex Hunter",
            customUserId = "cipher_fox",
            associatedIps = listOf("198.51.100.24"),
            currentIp = "198.51.100.24"
        )
        sessionManager.saveSession(profile)

        sessionManager.updateCurrentIp("203.0.113.88")

        val updated = sessionManager.getSavedUserProfile()
        assertEquals("203.0.113.88", updated?.currentIp)
        assertTrue(updated?.associatedIps?.contains("203.0.113.88") == true)
        assertTrue(updated?.associatedIps?.contains("198.51.100.24") == true)
    }

    @Test
    fun clearSessionRemovesAllData() {
        val profile = UserProfile(
            userId = "cipher_fox",
            displayName = "Alex",
            customUserId = "cipher_fox"
        )
        sessionManager.saveSession(profile)
        assertTrue(sessionManager.isLoggedIn())

        sessionManager.clearSession()

        assertFalse(sessionManager.isLoggedIn())
        assertNull(sessionManager.getSavedUserProfile())
    }
}
