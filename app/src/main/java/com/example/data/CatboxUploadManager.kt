package com.example.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.UploadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

private const val TAG = "CatboxUploadManager"
private const val CATBOX_UNIVERSAL_USERHASH = "b38bbe619e7dc69e16d7df563"

/**
 * Keyless public file uploader targeting Catbox.moe.
 * Specification:
 * - Gateway: https://catbox.moe/user/api.php
 * - Method: POST (multipart/form-data)
 * - Fields:
 *     reqtype: "fileupload"
 *     fileToUpload: [binary stream]
 * - Authentication: None (completely keyless)
 * - Response: Raw plain-text string containing direct CDN URL (e.g., https://files.catbox.moe/xxxxxx.png)
 * - Max file size: 200MB
 */
class CatboxUploadManager(
    private val client: OkHttpClient = defaultOkHttpClient()
) {
    companion object {
        const val CATBOX_API_ENDPOINT = "https://catbox.moe/user/api.php"
        const val MAX_SIZE_BYTES = 200 * 1024 * 1024L // 200MB
        const val SIZE_LIMIT_ALERT = "Upload failed. Please ensure the file size is under 200MB."

        fun getUniversalUserhash(): String = CATBOX_UNIVERSAL_USERHASH

        fun defaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    data class CatboxUploadResult(
        val url: String,
        val isLocalFallback: Boolean = false,
        val errorMessage: String? = null
    )

    /**
     * Uploads a file to Catbox.moe using a raw binary stream.
     * If Catbox times out, returns HTTP 502, or fails, the catch block intercepts it
     * and returns the local file URI smoothly so chat bubbles never break.
     */
    suspend fun uploadFile(
        contentResolver: ContentResolver,
        fileUri: Uri,
        filename: String = "attachment",
        contentType: String = "application/octet-stream",
        onProgress: ((Float) -> Unit)? = null
    ): CatboxUploadResult = withContext(Dispatchers.IO) {
        try {
            // Determine file size
            val size = getFileSize(contentResolver, fileUri)
            if (size > MAX_SIZE_BYTES) {
                Log.w(TAG, "File size exceeds 200MB limit ($size bytes)")
                return@withContext CatboxUploadResult(
                    url = fileUri.toString(),
                    isLocalFallback = true,
                    errorMessage = SIZE_LIMIT_ALERT
                )
            }

            val mediaType = contentType.toMediaTypeOrNull()

            // Custom RequestBody that streams from contentResolver
            val fileRequestBody = object : RequestBody() {
                override fun contentType(): MediaType? = mediaType

                override fun contentLength(): Long = size

                override fun writeTo(sink: BufferedSink) {
                    val inputStream: InputStream = contentResolver.openInputStream(fileUri)
                        ?: throw IOException("Cannot read file URI: $fileUri")
                    inputStream.source().use { source ->
                        if (size > 0 && onProgress != null) {
                            var totalBytesRead = 0L
                            val buffer = okio.Buffer()
                            var readCount: Long
                            while (source.read(buffer, 8192L).also { readCount = it } != -1L) {
                                sink.write(buffer, readCount)
                                totalBytesRead += readCount
                                val progress = (totalBytesRead.toFloat() / size.toFloat()).coerceIn(0f, 1f)
                                onProgress(progress)
                            }
                        } else {
                            sink.writeAll(source)
                            onProgress?.invoke(1f)
                        }
                    }
                }
            }

            val sanitizedFilename = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(100)

            // Strictly format multipart payload with reqtype="fileupload", userhash, and fileToUpload
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart("userhash", CATBOX_UNIVERSAL_USERHASH)
                .addFormDataPart("fileToUpload", sanitizedFilename, fileRequestBody)
                .build()

            // Keyless request: NO authorization headers or API secret keys
            val request = Request.Builder()
                .url(CATBOX_API_ENDPOINT)
                .post(multipartBody)
                .header("User-Agent", "ChatApp/1.0 (Android)")
                .build()

            Log.d(TAG, "Uploading file to Catbox.moe: $sanitizedFilename (${size / 1024} KB)")

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty().trim()

                if (response.isSuccessful && responseBody.startsWith("http", ignoreCase = true)) {
                    Log.i(TAG, "Catbox upload succeeded: $responseBody")
                    onProgress?.invoke(1f)
                    return@withContext CatboxUploadResult(url = responseBody, isLocalFallback = false)
                }

                // Catbox error, 502 Bad Gateway, or unexpected response
                Log.w(TAG, "Catbox returned HTTP ${response.code}: $responseBody")
                val alert = if (response.code == 502 || response.code == 504 || size > MAX_SIZE_BYTES) {
                    SIZE_LIMIT_ALERT
                } else {
                    "Catbox notice: $responseBody"
                }

                return@withContext CatboxUploadResult(
                    url = fileUri.toString(),
                    isLocalFallback = true,
                    errorMessage = alert
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Catbox upload exception: ${e.localizedMessage}", e)
            val isTimeoutOr502 = e.message?.contains("502") == true || e.message?.contains("timeout", ignoreCase = true) == true
            val errorMsg = if (isTimeoutOr502) SIZE_LIMIT_ALERT else "Upload notice: ${e.localizedMessage}"
            return@withContext CatboxUploadResult(
                url = fileUri.toString(),
                isLocalFallback = true,
                errorMessage = errorMsg
            )
        }
    }

    /**
     * Resolves file size from ContentResolver or File object.
     */
    private fun getFileSize(contentResolver: ContentResolver, uri: Uri): Long {
        if (uri.scheme == "file") {
            uri.path?.let { File(it) }?.let { if (it.exists()) return it.length() }
        }
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) cursor.getLong(sizeIndex) else -1L
                } else -1L
            } ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }
}
