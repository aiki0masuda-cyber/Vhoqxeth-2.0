package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class FirestoreConnectionState {
    object Idle : FirestoreConnectionState()
    object Connecting : FirestoreConnectionState()
    data class Connected(
        val databaseId: String,
        val latencyMs: Long,
        val confirmedTimestamp: String,
        val serverStatus: String
    ) : FirestoreConnectionState()
    data class Error(val message: String) : FirestoreConnectionState()
}

class FirestoreConnectionManager(private val context: Context) {
    private val tag = "FirestoreConnection"

    val databaseId: String = try {
        context.getString(R.string.firestore_database_id)
    } catch (e: Exception) {
        "default"
    }

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(databaseId)
    }

    suspend fun testDatabaseConnection(): FirestoreConnectionState = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val currentTimeStr = isoFormat.format(Date())

            val probePayload = hashMapOf<String, Any>(
                "status" to "CONNECTED",
                "databaseId" to databaseId,
                "clientApp" to "DAV InfoExchange",
                "timestamp" to currentTimeStr
            )

            val probeRef = db.collection("connection_probes").document("health_probe")

            // Send write request to Firestore
            probeRef.set(probePayload).await()

            // Send read request to Firestore to confirm round-trip data receipt
            val readSnapshot = probeRef.get().await()
            val latency = System.currentTimeMillis() - startTime

            if (readSnapshot.exists()) {
                val serverStatus = readSnapshot.getString("status") ?: "CONNECTED"
                val verifiedTime = readSnapshot.getString("timestamp") ?: currentTimeStr
                Log.i(tag, "Firestore database connected successfully ($databaseId) in ${latency}ms")
                FirestoreConnectionState.Connected(
                    databaseId = databaseId,
                    latencyMs = latency,
                    confirmedTimestamp = verifiedTime,
                    serverStatus = serverStatus
                )
            } else {
                FirestoreConnectionState.Error("Probe document was written but could not be read back")
            }
        } catch (e: Exception) {
            Log.e(tag, "Firestore connection request failed", e)
            FirestoreConnectionState.Error(e.localizedMessage ?: "Unknown Firestore connection error")
        }
    }
}
