package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.AdminRepository
import com.example.model.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdminRepositoryTest : FirestoreEmulatorTestBase() {

    private lateinit var adminRepository: AdminRepository

    @Before
    fun setUpAdmin() {
        adminRepository = AdminRepository(firestore)
    }

    @Test
    fun adminCanCreateAndRetrieveUsers(): Unit = runBlocking {
        val uniqueHandle = "admin_t_${System.currentTimeMillis()}"
        val createResult = adminRepository.createUser("Admin Test", uniqueHandle, "198.51.100.99")
        assertTrue("Create failed: ${createResult.exceptionOrNull()?.message}", createResult.isSuccess)

        val usersResult = adminRepository.getAllUsers()
        assertTrue("Get users failed: ${usersResult.exceptionOrNull()?.message}", usersResult.isSuccess)
        val users = usersResult.getOrThrow()
        assertTrue(users.any { it.customUserId == uniqueHandle })
    }

    @Test
    fun adminCanUpdateUserDetails(): Unit = runBlocking {
        val origHandle = "orig_${System.currentTimeMillis()}"
        val createRes = adminRepository.createUser("Original Name", origHandle, "198.51.100.50")
        assertTrue("Initial create failed: ${createRes.exceptionOrNull()?.message}", createRes.isSuccess)

        val updated = UserProfile(
            userId = origHandle,
            displayName = "Updated Name",
            customUserId = origHandle,
            associatedIps = listOf("198.51.100.50", "203.0.113.1"),
            currentIp = "203.0.113.1"
        )
        val updateRes = adminRepository.updateUser(origHandle, updated)
        assertTrue("Update failed: ${updateRes.exceptionOrNull()?.message}", updateRes.isSuccess)

        val users = adminRepository.getAllUsers().getOrThrow()
        val user = users.first { it.customUserId == origHandle }
        assertEquals("Updated Name", user.displayName)
        assertTrue(user.associatedIps.contains("203.0.113.1"))
    }

    @Test
    fun adminCanDeleteUserAndUnlinkIps(): Unit = runBlocking {
        val delHandle = "del_${System.currentTimeMillis()}"
        val testIp = "198.51.100.${(10..200).random()}"
        val createRes = adminRepository.createUser("To Delete", delHandle, testIp)
        assertTrue("Initial create failed: ${createRes.exceptionOrNull()?.message}", createRes.isSuccess)

        val deleteRes = adminRepository.deleteUser(delHandle, listOf(testIp))
        assertTrue("Delete failed: ${deleteRes.exceptionOrNull()?.message}", deleteRes.isSuccess)

        val users = adminRepository.getAllUsers().getOrThrow()
        assertFalse(users.any { it.customUserId == delHandle })

        val ips = adminRepository.getAllRegisteredIps().getOrThrow()
        assertFalse(ips.any { it.ip == testIp })
    }
}
