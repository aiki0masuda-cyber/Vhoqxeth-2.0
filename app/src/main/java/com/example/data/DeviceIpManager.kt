package com.example.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DeviceIpManager(private val context: Context) {
    private val tag = "DeviceIpManager"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves the current device IP address by querying a public IP discovery endpoint.
     * Falls back to a deterministic Android hardware identifier if network IP discovery is unreachable.
     */
    suspend fun resolveDeviceIp(): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.ipify.org?format=json")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val publicIp = json.optString("ip")
                if (publicIp.isNotBlank()) {
                    Log.i(tag, "Resolved public IP: $publicIp")
                    return@withContext publicIp
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Public IP lookup failed, falling back to device identity: ${e.message}")
        }

        // Hardware device ID fallback
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }

        val fallbackId = if (!androidId.isNullOrBlank()) {
            "DEV-" + androidId.take(8).uppercase()
        } else {
            "DEV-192.168.1.1"
        }
        Log.i(tag, "Using fallback device IP identifier: $fallbackId")
        fallbackId
    }

    companion object {
        /**
         * Sanitizes IP addresses or IPv6 strings into Firestore-safe document identifiers.
         */
        fun sanitizeIpKey(ip: String): String {
            return ip.trim()
                .replace(".", "_")
                .replace(":", "_")
                .replace("/", "_")
                .replace(" ", "")
        }
    }
}
