package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirestoreRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun unauthenticatedReadOfConnectionProbesSucceeds(): Unit = runBlocking {
    val probeDoc = firestore.collection("connection_probes").document("health_probe")
    try {
      val snapshot = probeDoc.get().await()
      assertNotNull(snapshot)
    } catch (e: Exception) {
      fail("Unauthenticated read of connection_probes should succeed: ${e.message}")
    }
  }

  @Test
  fun validConnectionProbeWriteSucceeds(): Unit = runBlocking {
    val probeDoc = firestore.collection("connection_probes").document("health_check_test")
    val payload = mapOf(
      "status" to "CONNECTED",
      "databaseId" to databaseId,
      "clientApp" to "Vhoqxeth Android"
    )
    try {
      probeDoc.set(payload).await()
    } catch (e: Exception) {
      fail("Valid connection probe write should succeed: ${e.message}")
    }
  }

  @Test
  fun unauthenticatedReadOfRegisteredIpsSucceeds(): Unit = runBlocking {
    val ipDoc = firestore.collection("registered_ips").document("192_168_1_100")
    try {
      val snapshot = ipDoc.get().await()
      assertNotNull(snapshot)
    } catch (e: Exception) {
      fail("Unauthenticated read of registered_ips should succeed: ${e.message}")
    }
  }

  @Test
  fun unauthenticatedWriteOfRegisteredIpSucceeds(): Unit = runBlocking {
    val ipDoc = firestore.collection("registered_ips").document("192_168_1_100")
    val payload = mapOf(
      "ip" to "192.168.1.100",
      "userId" to "alice_handle",
      "customUserId" to "alice_handle",
      "displayName" to "Alice"
    )
    try {
      ipDoc.set(payload).await()
      val snapshot = ipDoc.get().await()
      assertTrue(snapshot.exists())
      assertEquals("alice_handle", snapshot.getString("customUserId"))
    } catch (e: Exception) {
      fail("Write to registered_ips should succeed: ${e.message}")
    }
  }

  @Test
  fun unauthenticatedReadAndWriteOfUserProfileSucceeds(): Unit = runBlocking {
    val userDoc = firestore.collection("users").document("alice_handle")
    val userPayload = mapOf(
      "userId" to "alice_handle",
      "displayName" to "Alice",
      "customUserId" to "alice_handle"
    )
    try {
      userDoc.set(userPayload).await()
      val snapshot = userDoc.get().await()
      assertTrue(snapshot.exists())
      assertEquals("Alice", snapshot.getString("displayName"))
      assertEquals("alice_handle", snapshot.getString("customUserId"))
    } catch (e: Exception) {
      fail("Writing and reading user profile should succeed: ${e.message}")
    }
  }

  @Test
  fun deletionOfUserProfileAndRegisteredIpSucceeds(): Unit = runBlocking {
    val userDoc = firestore.collection("users").document("del_user")
    val ipDoc = firestore.collection("registered_ips").document("del_ip")

    userDoc.set(
      mapOf(
        "userId" to "del_user",
        "displayName" to "To Delete",
        "customUserId" to "del_user"
      )
    ).await()

    ipDoc.set(
      mapOf(
        "ip" to "10.0.0.1",
        "userId" to "del_user",
        "customUserId" to "del_user",
        "displayName" to "To Delete"
      )
    ).await()

    try {
      userDoc.delete().await()
      ipDoc.delete().await()
      assertFalse(userDoc.get().await().exists())
      assertFalse(ipDoc.get().await().exists())
    } catch (e: Exception) {
      fail("Deleting user profile and registered IP should succeed: ${e.message}")
    }
  }

  @Test
  fun invalidUserProfileWriteFails(): Unit = runBlocking {
    val userDoc = firestore.collection("users").document("bad_user")
    val badPayload = mapOf(
      "userId" to "bad_user",
      "displayName" to "No Handle"
      // missing customUserId
    )
    try {
      userDoc.set(badPayload).await()
      fail("Invalid user profile write should fail with PERMISSION_DENIED")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }
}
