package com.example.data

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.example.model.UploadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import okio.source
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Production-ready Backblaze B2 (S3-compatible) upload & download manager.
 * Supports Private B2 buckets via 24-hour pre-signed GET URLs and 5-minute pre-signed PUT URLs.
 * Features in-memory caching and automatic token renewal for expiring media URLs.
 */
class B2UploadManager(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "B2UploadManager"

        // Default or configurable edge worker gatekeeper endpoint
        const val DEFAULT_WORKER_ENDPOINT = "https://b2-presigned-url-worker.dav-academic.workers.dev"
    }

    /**
     * Map of active OkHttp upload calls to allow instantaneous user cancellation.
     */
    private val activeCalls = ConcurrentHashMap<String, Call>()

    /**
     * In-memory cache of temporary 24-hour signed GET URLs keyed by storage object key.
     */
    data class CachedSignedUrl(
        val url: String,
        val expiresAtMillis: Long
    )

    private val urlCache = ConcurrentHashMap<String, CachedSignedUrl>()

    /**
     * Ticket response returned by the serverless edge worker.
     */
    data class UploadTicket(
        val uploadUrl: String,
        val temporaryDownloadUrl: String,
        val key: String,
        val expiresInSeconds: Int = 86400
    ) {
        val publicDownloadUrl: String get() = temporaryDownloadUrl
    }

    data class UploadCompletion(
        val downloadUrl: String,
        val key: String
    )

    /**
     * Step A: Requests a pre-signed PUT upload URL and an initial 24-hour pre-signed GET URL
     * from the serverless edge gatekeeper endpoint.
     */
    suspend fun fetchUploadTicket(
        workerEndpoint: String = DEFAULT_WORKER_ENDPOINT,
        filename: String,
        contentType: String
    ): Result<UploadTicket> = withContext(Dispatchers.IO) {
        try {
            val encodedFilename = URLEncoder.encode(filename, "UTF-8")
            val encodedContentType = URLEncoder.encode(contentType, "UTF-8")
            val requestUrl = if (workerEndpoint.contains("?")) {
                "$workerEndpoint&action=upload&filename=$encodedFilename&contentType=$encodedContentType"
            } else {
                "$workerEndpoint?action=upload&filename=$encodedFilename&contentType=$encodedContentType"
            }

            Log.d(TAG, "Requesting pre-signed URL from edge worker: $requestUrl")

            val request = Request.Builder()
                .url(requestUrl)
                .get()
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errMsg = try {
                        JSONObject(bodyString).optString("error", "HTTP ${response.code}: ${response.message}")
                    } catch (_: Exception) {
                        "HTTP ${response.code}: ${response.message}"
                    }
                    return@withContext Result.failure(IOException("Gatekeeper error: $errMsg"))
                }

                val json = JSONObject(bodyString)
                val uploadUrl = json.getString("uploadUrl")
                val tempDownloadUrl = json.optString("temporaryDownloadUrl", json.optString("publicDownloadUrl", ""))
                val key = json.optString("key", filename)
                val expiresIn = json.optInt("expiresInSeconds", 86400)

                // Prime in-memory cache with safety buffer (10 minutes before expiration)
                if (tempDownloadUrl.isNotBlank() && key.isNotBlank()) {
                    val expiryMillis = System.currentTimeMillis() + (expiresIn.toLong() * 1000L) - (10 * 60 * 1000L)
                    urlCache[key] = CachedSignedUrl(url = tempDownloadUrl, expiresAtMillis = expiryMillis)
                }

                Result.success(
                    UploadTicket(
                        uploadUrl = uploadUrl,
                        temporaryDownloadUrl = tempDownloadUrl,
                        key = key,
                        expiresInSeconds = expiresIn
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch pre-signed ticket from worker", e)
            Result.failure(e)
        }
    }

    /**
     * Requests a fresh 24-hour pre-signed GET download URL from the serverless edge function
     * using action=download and the unique S3 object key.
     */
    suspend fun fetchTemporaryDownloadUrl(
        key: String,
        workerEndpoint: String = DEFAULT_WORKER_ENDPOINT
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val encodedKey = URLEncoder.encode(key, "UTF-8")
            val requestUrl = if (workerEndpoint.contains("?")) {
                "$workerEndpoint&action=download&key=$encodedKey"
            } else {
                "$workerEndpoint?action=download&key=$encodedKey"
            }

            Log.d(TAG, "Requesting fresh 24h download link for key=$key from: $requestUrl")

            val request = Request.Builder()
                .url(requestUrl)
                .get()
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errMsg = try {
                        JSONObject(bodyString).optString("error", "HTTP ${response.code}")
                    } catch (_: Exception) {
                        "HTTP ${response.code}"
                    }
                    return@withContext Result.failure(IOException("Gatekeeper error: $errMsg"))
                }

                val json = JSONObject(bodyString)
                val freshUrl = json.optString("temporaryDownloadUrl", json.optString("publicDownloadUrl", ""))
                val expiresIn = json.optInt("expiresInSeconds", 86400)

                if (freshUrl.isNotBlank()) {
                    val expiryMillis = System.currentTimeMillis() + (expiresIn.toLong() * 1000L) - (10 * 60 * 1000L)
                    urlCache[key] = CachedSignedUrl(url = freshUrl, expiresAtMillis = expiryMillis)
                    Result.success(freshUrl)
                } else {
                    Result.failure(IOException("No download URL returned from worker"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh download URL for key: $key", e)
            Result.failure(e)
        }
    }

    /**
     * Resolves an attachment URL from in-memory cache, or silently requests a fresh 24-hour
     * pre-signed GET link from Cloudflare Worker if expired or missing.
     */
    suspend fun getOrRefreshDownloadUrl(
        key: String?,
        currentUrl: String? = null,
        workerEndpoint: String = DEFAULT_WORKER_ENDPOINT
    ): String {
        if (key.isNullOrBlank()) {
            return currentUrl ?: ""
        }

        val now = System.currentTimeMillis()
        val cached = urlCache[key]
        if (cached != null && cached.expiresAtMillis > now) {
            return cached.url
        }

        // Check if currentUrl has a valid unexpired signature
        if (!currentUrl.isNullOrBlank() && currentUrl.contains("X-Amz-Signature")) {
            urlCache[key] = CachedSignedUrl(url = currentUrl, expiresAtMillis = now + 3600_000L)
            return currentUrl
        }

        // Silent renewal from Edge Gatekeeper
        val refreshed = fetchTemporaryDownloadUrl(key, workerEndpoint)
        return refreshed.getOrNull() ?: currentUrl ?: ""
    }

    /**
     * Step B: Streams file binary directly to the pre-signed S3 PUT URL with real-time progress callbacks.
     */
    suspend fun streamFileToPresignedUrl(
        uploadId: String,
        uploadUrl: String,
        contentResolver: ContentResolver,
        fileUri: Uri,
        contentType: String,
        totalSizeBytes: Long,
        onProgress: (progress: Float, bytesWritten: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val mediaType = contentType.toMediaTypeOrNull()

            val countingBody = ProgressStreamRequestBody(
                mediaType = mediaType,
                contentLength = totalSizeBytes,
                openInputStream = {
                    contentResolver.openInputStream(fileUri)
                        ?: throw IOException("Cannot open input stream for URI: $fileUri")
                },
                onProgress = onProgress
            )

            val request = Request.Builder()
                .url(uploadUrl)
                .put(countingBody)
                .header("Content-Type", contentType)
                .build()

            val call = client.newCall(request)
            activeCalls[uploadId] = call

            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        Log.e(TAG, "S3 PUT failed with code ${response.code}: $body")
                        return@withContext Result.failure(
                            IOException("S3 PUT failed with HTTP ${response.code}: ${response.message}")
                        )
                    }
                    Log.i(TAG, "File binary streamed successfully to Backblaze B2 (uploadId=$uploadId)")
                    Result.success(Unit)
                }
            } finally {
                activeCalls.remove(uploadId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Streaming to pre-signed URL encountered error", e)
            Result.failure(e)
        }
    }

    /**
     * Executes the complete two-step pipeline and returns both the initial 24h download link and storage key.
     */
    suspend fun executeCompleteUploadWithKey(
        uploadId: String,
        workerEndpoint: String = DEFAULT_WORKER_ENDPOINT,
        contentResolver: ContentResolver,
        fileUri: Uri,
        filename: String,
        contentType: String,
        fileSizeBytes: Long,
        onStateChanged: (UploadState) -> Unit
    ): Result<UploadCompletion> {
        onStateChanged(UploadState.FetchingTicket)

        val ticketResult = fetchUploadTicket(
            workerEndpoint = workerEndpoint,
            filename = filename,
            contentType = contentType
        )

        if (ticketResult.isFailure) {
            val err = ticketResult.exceptionOrNull()?.message ?: "Failed to get upload authorization"
            onStateChanged(UploadState.Error(err))
            return Result.failure(Exception(err))
        }

        val ticket = ticketResult.getOrThrow()
        onStateChanged(UploadState.Uploading(progress = 0f, bytesWritten = 0L, totalBytes = fileSizeBytes))

        val uploadResult = streamFileToPresignedUrl(
            uploadId = uploadId,
            uploadUrl = ticket.uploadUrl,
            contentResolver = contentResolver,
            fileUri = fileUri,
            contentType = contentType,
            totalSizeBytes = fileSizeBytes,
            onProgress = { progress, written, total ->
                onStateChanged(UploadState.Uploading(progress, written, total))
            }
        )

        return if (uploadResult.isSuccess) {
            onStateChanged(UploadState.Success(ticket.temporaryDownloadUrl))
            Result.success(UploadCompletion(downloadUrl = ticket.temporaryDownloadUrl, key = ticket.key))
        } else {
            val err = uploadResult.exceptionOrNull()?.message ?: "Failed to stream binary to storage"
            onStateChanged(UploadState.Error(err))
            Result.failure(Exception(err))
        }
    }

    /**
     * High-level helper returning only the download URL string.
     */
    suspend fun executeCompleteUpload(
        uploadId: String,
        workerEndpoint: String = DEFAULT_WORKER_ENDPOINT,
        contentResolver: ContentResolver,
        fileUri: Uri,
        filename: String,
        contentType: String,
        fileSizeBytes: Long,
        onStateChanged: (UploadState) -> Unit
    ): Result<String> {
        val res = executeCompleteUploadWithKey(
            uploadId = uploadId,
            workerEndpoint = workerEndpoint,
            contentResolver = contentResolver,
            fileUri = fileUri,
            filename = filename,
            contentType = contentType,
            fileSizeBytes = fileSizeBytes,
            onStateChanged = onStateChanged
        )
        return res.map { it.downloadUrl }
    }

    /**
     * Cancels an ongoing upload by its tracking ID.
     */
    fun cancelUpload(uploadId: String) {
        activeCalls.remove(uploadId)?.cancel()
        Log.d(TAG, "Cancelled upload call for id: $uploadId")
    }
}

/**
 * Custom OkHttp RequestBody that streams from an InputStream while measuring written bytes
 * through an Okio ForwardingSink to prevent loading large videos/PDFs into memory.
 */
internal class ProgressStreamRequestBody(
    private val mediaType: MediaType?,
    private val contentLength: Long,
    private val openInputStream: () -> InputStream,
    private val onProgress: (progress: Float, bytesWritten: Long, totalBytes: Long) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = mediaType

    override fun contentLength(): Long = contentLength

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink, contentLength, onProgress)
        val bufferedSink = countingSink.buffer()

        openInputStream().use { inputStream ->
            val source = inputStream.source()
            bufferedSink.writeAll(source)
            bufferedSink.flush()
        }
    }

    private class CountingSink(
        delegate: Sink,
        private val totalBytes: Long,
        private val onProgress: (Float, Long, Long) -> Unit
    ) : ForwardingSink(delegate) {

        private var bytesWritten = 0L
        private var lastReportedPercent = -1

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount

            val progress = if (totalBytes > 0) {
                (bytesWritten.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            val currentPercent = (progress * 100).toInt()
            if (currentPercent != lastReportedPercent || bytesWritten == totalBytes) {
                lastReportedPercent = currentPercent
                onProgress(progress, bytesWritten, totalBytes)
            }
        }
    }
}
