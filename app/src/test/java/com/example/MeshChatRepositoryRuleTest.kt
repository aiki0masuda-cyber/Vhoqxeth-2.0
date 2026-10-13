package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.MeshChatRepository
import com.example.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MeshChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private lateinit var chatRepository: MeshChatRepository

    @Before
    fun setUpRepo() {
        chatRepository = MeshChatRepository(firestore)
    }

    @Test
    fun deterministicThreadIdGeneration(): Unit {
        val id1 = chatRepository.generateThreadId("Alice", "Bob")
        val id2 = chatRepository.generateThreadId("bob", "alice")
        assertEquals(id1, id2)
        assertEquals("thread_alice_bob", id1)
    }

    @Test
    fun searchUserByHandleFindsRegisteredUser(): Unit = runBlocking {
        val handle = "search_user_${System.currentTimeMillis()}"
        val userPayload = mapOf(
            "userId" to handle,
            "displayName" to "Search User",
            "customUserId" to handle,
            "currentIp" to "192.168.1.99"
        )
        firestore.collection("users").document(handle).set(userPayload).await()

        val found = chatRepository.searchUserByHandle(handle).getOrThrow()
        assertNotNull(found)
        assertEquals(handle, found?.customUserId)
        assertEquals("Search User", found?.displayName)

        val notFound = chatRepository.searchUserByHandle("non_existent_random_xyz").getOrThrow()
        assertNull(notFound)
    }

    @Test
    fun addContactCreatesReciprocalContactsAndSharedThread(): Unit = runBlocking {
        val userA = UserProfile(
            userId = "user_a_${System.currentTimeMillis()}",
            displayName = "User Alpha",
            customUserId = "user_a_${System.currentTimeMillis()}",
            currentIp = "10.0.0.1"
        )
        val userB = UserProfile(
            userId = "user_b_${System.currentTimeMillis()}",
            displayName = "User Beta",
            customUserId = "user_b_${System.currentTimeMillis()}",
            currentIp = "10.0.0.2"
        )

        val addResult = chatRepository.addContact(userA, userB)
        assertTrue("Add contact failed: ${addResult.exceptionOrNull()?.stackTraceToString()}", addResult.isSuccess)

        val expectedThreadId = chatRepository.generateThreadId(userA.customUserId, userB.customUserId)
        assertEquals(expectedThreadId, addResult.getOrThrow().id)

        // Verify contact in User A's roster
        val contactDocA = firestore.collection("users")
            .document(userA.customUserId)
            .collection("contacts")
            .document(userB.customUserId)
            .get()
            .await()
        assertTrue(contactDocA.exists())
        assertEquals(userB.displayName, contactDocA.getString("displayName"))

        // Verify reciprocal contact in User B's roster
        val contactDocB = firestore.collection("users")
            .document(userB.customUserId)
            .collection("contacts")
            .document(userA.customUserId)
            .get()
            .await()
        assertTrue(contactDocB.exists())
        assertEquals(userA.displayName, contactDocB.getString("displayName"))

        // Verify thread document in /threads/{threadId}
        val threadDoc = firestore.collection("threads").document(expectedThreadId).get().await()
        assertTrue(threadDoc.exists())
        @Suppress("UNCHECKED_CAST")
        val participants = threadDoc.get("participantIds") as List<String>
        assertTrue(participants.contains(userA.customUserId))
        assertTrue(participants.contains(userB.customUserId))
    }

    @Test
    fun sendMessageUpdatesThreadAndWritesSubcollection(): Unit = runBlocking {
        val u1 = "sender_${System.currentTimeMillis()}"
        val u2 = "receiver_${System.currentTimeMillis()}"
        val profile1 = UserProfile(
            userId = u1,
            displayName = "Sender 1",
            customUserId = u1
        )
        val profile2 = UserProfile(
            userId = u2,
            displayName = "Receiver 2",
            customUserId = u2
        )

        val threadResult = chatRepository.addContact(profile1, profile2)
        val thread = threadResult.getOrThrow()

        val sendResult = chatRepository.sendMessage(thread.id, profile1, "Hello quantum mesh!")
        assertTrue("Send message failed: ${sendResult.exceptionOrNull()?.message}", sendResult.isSuccess)
        val message = sendResult.getOrThrow()
        assertEquals("Hello quantum mesh!", message.content)
        assertEquals(u1, message.senderId)

        // Verify thread lastMessage updated
        val threadDoc = firestore.collection("threads").document(thread.id).get().await()
        assertEquals("Hello quantum mesh!", threadDoc.getString("lastMessage"))
        assertEquals(u1, threadDoc.getString("lastSenderId"))
    }
}
