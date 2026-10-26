package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

object FileAttachmentActionManager {

    private const val TAG = "FileAttachmentAction"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Resolves the exact MIME type based on file extension.
     */
    fun getMimeType(fileName: String, fallbackType: String = "*/*"): String {
        val ext = fileName.substringAfterLast('.', "").lowercase().trim()
        return when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "svg" -> "image/svg+xml"
            "pdf" -> "application/pdf"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            "json" -> "application/json"
            "zip" -> "application/zip"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            else -> when (fallbackType.uppercase()) {
                "IMAGE" -> "image/jpeg"
                "VIDEO" -> "video/mp4"
                "PDF" -> "application/pdf"
                else -> "*/*"
            }
        }
    }

    /**
     * ACTION TYPE 1: DIRECT FILE VIEWING (In-App / Browser Native Opening)
     * Opens direct URL in clean web browser window or local URI with explicit MIME type.
     * Avoids triggering the generic Android chooser overlay.
     */
    fun openAttachment(
        context: Context,
        urlOrUri: String,
        fileName: String,
        fallbackType: String = "*/*"
    ) {
        if (urlOrUri.isBlank()) {
            Toast.makeText(context, "Attachment link is unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val parsedUri = Uri.parse(urlOrUri)
            val isRemote = urlOrUri.startsWith("http://", ignoreCase = true) ||
                    urlOrUri.startsWith("https://", ignoreCase = true)

            if (isRemote) {
                // Open directly in a clean external web browser window without system app selector
                val browserIntent = Intent(Intent.ACTION_VIEW, parsedUri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            } else {
                // Local cached URI: explicitly assign MIME type based on file extension
                val mime = getMimeType(fileName, fallbackType)
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(parsedUri, mime)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(viewIntent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct launch failed, attempting fallback: ${e.message}")
            try {
                // Generic fallback without chooser
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlOrUri)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (err: Exception) {
                Log.e(TAG, "Cannot open attachment: ${err.message}", err)
                Toast.makeText(context, "Cannot open file: ${err.localizedMessage ?: "No supported application found"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * ACTION TYPE 2: PHYSICAL SYSTEM FOLDER DOWNLOAD
     * Fetches raw binary file stream from remote Catbox URL via HTTP GET and writes bytes
     * directly into the local physical device Download folder, then alerts the user.
     */
    suspend fun downloadToDownloadsFolder(
        context: Context,
        urlOrUri: String,
        suggestedFileName: String,
        fallbackType: String = "*/*"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (urlOrUri.isBlank()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Cannot download: attachment link is empty", Toast.LENGTH_SHORT).show()
            }
            return@withContext Result.failure(IllegalArgumentException("Empty URL or URI"))
        }

        val sanitizedName = suggestedFileName.ifBlank { "attachment_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(120)

        val mimeType = getMimeType(sanitizedName, fallbackType)

        try {
            val isRemote = urlOrUri.startsWith("http://", ignoreCase = true) ||
                    urlOrUri.startsWith("https://", ignoreCase = true)

            var inputStream: InputStream? = null

            if (isRemote) {
                val request = Request.Builder()
                    .url(urlOrUri)
                    .header("User-Agent", "ChatApp/1.0 (Android)")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val code = response.code
                    response.close()
                    throw IOException("HTTP $code while downloading file")
                }
                inputStream = response.body?.byteStream()
                    ?: throw IOException("Empty response payload from server")
            } else {
                inputStream = context.contentResolver.openInputStream(Uri.parse(urlOrUri))
                    ?: throw IOException("Cannot read local file stream: $urlOrUri")
            }

            inputStream.use { input ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizedName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    val resolver = context.contentResolver
                    val downloadUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        ?: throw IOException("Failed to create download entry in MediaStore")

                    try {
                        resolver.openOutputStream(downloadUri)?.use { output ->
                            input.copyTo(output)
                            output.flush()
                        } ?: throw IOException("Failed to open output stream to MediaStore")

                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(downloadUri, contentValues, null, null)
                    } catch (e: Exception) {
                        resolver.delete(downloadUri, null, null)
                        throw e
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    val targetFile = File(downloadsDir, sanitizedName)
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "File saved successfully to Downloads folder.", Toast.LENGTH_SHORT).show()
            }

            Log.i(TAG, "File successfully saved to Downloads: $sanitizedName")
            Result.success(sanitizedName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading file to Downloads folder: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download failed: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
            Result.failure(e)
        }
    }
}
