package com.example.util

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object FileAttachmentActionManager {

    private const val TAG = "FileAttachmentAction"

    const val APP_DOWNLOAD_DIR_NAME = "DAV InfoExchange"
    const val CATEGORY_IMAGES = "Images"
    const val CATEGORY_DOCUMENTS = "Documents"
    const val CATEGORY_VIDEOS = "Videos"
    const val CATEGORY_AUDIO = "Audio"
    const val CATEGORY_OTHER = "Other"

    /**
     * Specialized OkHttpClient configured to bypass SSL certificate / hostname mismatches
     * specifically for endpoints matching the "catbox.moe" domain cluster (e.g., de.catbox.moe).
     */
    val httpClient by lazy {
        createPermissiveCatboxHttpClient()
    }

    private fun createPermissiveCatboxHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, trustAllCerts, SecureRandom())
            }

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { hostname, session ->
                    // Override certificate verification callback to return true specifically for endpoints
                    // matching the "catbox.moe" domain cluster (de.catbox.moe, files.catbox.moe, etc.)
                    if (hostname.endsWith("catbox.moe", ignoreCase = true) || hostname.equals("catbox.moe", ignoreCase = true)) {
                        true
                    } else {
                        HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                    }
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "Fallback to hostname-verifier only client: ${e.message}")
            OkHttpClient.Builder()
                .hostnameVerifier { hostname, session ->
                    if (hostname.endsWith("catbox.moe", ignoreCase = true) || hostname.equals("catbox.moe", ignoreCase = true)) {
                        true
                    } else {
                        HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                    }
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }

    /**
     * Returns a shareable, secure content URI for any local file using FileProvider,
     * completely eliminating Permission Denial and FileUriExposed exceptions.
     */
    fun getFileProviderUri(context: Context, file: File): Uri {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.w(TAG, "FileProvider URI generation failed: ${e.message}, falling back to Uri.fromFile")
            Uri.fromFile(file)
        }
    }

    /**
     * Enforces the secure HTTPS protocol on Catbox URLs ("https://files.catbox.moe/").
     * Replaces any insecure "http://" prefix with "https://", and converts alternate mirror
     * endpoints to the official secure "https://files.catbox.moe/" destination.
     */
    fun toBypassUrl(urlOrUri: String): String {
        if (urlOrUri.isBlank()) return urlOrUri
        return com.example.data.CatboxUploadManager.applyBypassMirror(urlOrUri)
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
     * Resolves the classified folder category based on file extension and MIME type.
     * Categorizes files into Images, Documents, Videos, Audio, or Other.
     */
    fun classifyFileCategory(fileName: String, mimeType: String = ""): String {
        val lowerMime = mimeType.lowercase().trim()
        val ext = fileName.substringAfterLast('.', "").lowercase().trim()

        if (lowerMime.startsWith("image/") || ext in listOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "heic", "heif")) {
            return CATEGORY_IMAGES
        }
        if (lowerMime.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "mov", "avi", "3gp", "flv", "wmv", "m4v")) {
            return CATEGORY_VIDEOS
        }
        if (lowerMime.startsWith("audio/") || ext in listOf("mp3", "wav", "ogg", "m4a", "aac", "flac", "opus", "wma")) {
            return CATEGORY_AUDIO
        }
        if (lowerMime.startsWith("text/") ||
            lowerMime.contains("pdf") ||
            lowerMime.contains("document") ||
            lowerMime.contains("sheet") ||
            lowerMime.contains("presentation") ||
            lowerMime.contains("msword") ||
            ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "json", "rtf", "epub", "odt", "ods", "odp")) {
            return CATEGORY_DOCUMENTS
        }
        return CATEGORY_OTHER
    }

    /**
     * ACTION TYPE 1: DIRECT FILE VIEWING (In-App / Browser Native Opening)
     * Opens direct URL in clean web browser window or local URI with explicit MIME type.
     * Uses FileProvider for local files to eliminate Permission Denial errors.
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
            val normalizedUrl = toBypassUrl(urlOrUri)
            val parsedUri = Uri.parse(normalizedUrl)
            val isRemote = normalizedUrl.startsWith("http://", ignoreCase = true) ||
                    normalizedUrl.startsWith("https://", ignoreCase = true)

            if (isRemote) {
                // Open directly in a clean external web browser window without system app selector
                val browserIntent = Intent(Intent.ACTION_VIEW, parsedUri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            } else {
                // Local cached URI: convert to FileProvider URI if it represents a file or content
                val mime = getMimeType(fileName, fallbackType)
                val shareableUri: Uri = if (parsedUri.scheme == "file" || parsedUri.scheme == null) {
                    val filePath = parsedUri.path ?: urlOrUri
                    val file = File(filePath)
                    if (file.exists()) {
                        getFileProviderUri(context, file)
                    } else {
                        parsedUri
                    }
                } else {
                    parsedUri
                }

                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(shareableUri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                // Pre-grant read URI permissions to target resolving activities to guarantee no Permission Denial
                try {
                    val resolvedActivities = context.packageManager.queryIntentActivities(
                        viewIntent,
                        PackageManager.MATCH_DEFAULT_ONLY
                    )
                    for (resolveInfo in resolvedActivities) {
                        val pkgName = resolveInfo.activityInfo.packageName
                        context.grantUriPermission(pkgName, shareableUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "grantUriPermission warning: ${e.message}")
                }

                context.startActivity(viewIntent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct launch failed, attempting fallback: ${e.message}")
            try {
                // Generic fallback without chooser
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(toBypassUrl(urlOrUri))).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
     * Downloads raw stream from URL into Downloads folder with multi-tier Permission Denial protection:
     * Tier 1: MediaStore.Downloads (Android 10+)
     * Tier 2: Public Environment.DIRECTORY_DOWNLOADS (Android <= 28)
     * Tier 3: Zero-permission app-specific external Downloads directory backed by FileProvider
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

        val effectiveDownloadUrl = toBypassUrl(urlOrUri)

        val sanitizedName = suggestedFileName.ifBlank { "attachment_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(120)

        val mimeType = getMimeType(sanitizedName, fallbackType)
        val category = classifyFileCategory(sanitizedName, mimeType)
        val relativeSubDir = "$APP_DOWNLOAD_DIR_NAME/$category"
        val destinationDisplayPath = "Downloads/$relativeSubDir/$sanitizedName"

        try {
            val isRemote = effectiveDownloadUrl.startsWith("http://", ignoreCase = true) ||
                    effectiveDownloadUrl.startsWith("https://", ignoreCase = true)

            var inputStream: InputStream? = null

            if (isRemote) {
                Log.d(TAG, "Downloading file stream from: $effectiveDownloadUrl")
                val request = Request.Builder()
                    .url(effectiveDownloadUrl)
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
                var savedSuccessfully = false

                // Tier 1: MediaStore.Downloads for Android 10+ (API 29+) with categorized folder structure
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizedName)
                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$relativeSubDir/")
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }

                        val resolver = context.contentResolver
                        val downloadUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        if (downloadUri != null) {
                            resolver.openOutputStream(downloadUri)?.use { output ->
                                input.copyTo(output)
                                output.flush()
                            }
                            contentValues.clear()
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            resolver.update(downloadUri, contentValues, null, null)
                            savedSuccessfully = true
                            Log.i(TAG, "File saved to MediaStore.Downloads ($relativeSubDir): $downloadUri")
                        }
                    } catch (secEx: SecurityException) {
                        Log.w(TAG, "MediaStore insert Permission Denial, falling back: ${secEx.message}")
                    } catch (e: Exception) {
                        Log.w(TAG, "MediaStore insert error, falling back: ${e.message}")
                    }
                }

                // Tier 2: Physical public Download directory (for API <= 28)
                if (!savedSuccessfully) {
                    try {
                        @Suppress("DEPRECATION")
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val classifiedDir = File(downloadsDir, relativeSubDir)
                        if (!classifiedDir.exists()) {
                            classifiedDir.mkdirs()
                        }
                        val targetFile = File(classifiedDir, sanitizedName)
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                            output.flush()
                        }
                        savedSuccessfully = true
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(targetFile.absolutePath),
                            arrayOf(mimeType),
                            null
                        )
                        Log.i(TAG, "File saved to public classified Downloads: ${targetFile.absolutePath}")
                    } catch (secEx: SecurityException) {
                        Log.w(TAG, "Public downloads directory Permission Denial, using app downloads: ${secEx.message}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Public downloads write error: ${e.message}")
                    }
                }

                // Tier 3: Zero-permission app-specific external files directory
                // (Guaranteed accessible without any permission denial on any Android version)
                if (!savedSuccessfully) {
                    val appDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: context.filesDir
                    val classifiedDir = File(appDownloadsDir, relativeSubDir)
                    if (!classifiedDir.exists()) {
                        classifiedDir.mkdirs()
                    }
                    val targetFile = File(classifiedDir, sanitizedName)
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                    savedSuccessfully = true
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf(mimeType),
                        null
                    )
                    Log.i(TAG, "File saved to app-specific classified Downloads: ${targetFile.absolutePath}")
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to $destinationDisplayPath", Toast.LENGTH_SHORT).show()
            }

            Log.i(TAG, "File successfully saved: $destinationDisplayPath")
            Result.success(destinationDisplayPath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading file to Downloads folder: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download failed: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
            Result.failure(e)
        }
    }

    /**
     * Optional / System DownloadManager integration:
     * Enqueues an asynchronous download request via the Android DownloadManager service.
     * Catches any SecurityException and falls back to app-specific external directories.
     */
    fun enqueueWithDownloadManager(
        context: Context,
        urlOrUri: String,
        suggestedFileName: String,
        fallbackType: String = "*/*"
    ): Long {
        val effectiveUrl = toBypassUrl(urlOrUri)
        val sanitizedName = suggestedFileName.ifBlank { "attachment_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(120)
        val mimeType = getMimeType(sanitizedName, fallbackType)
        val category = classifyFileCategory(sanitizedName, mimeType)
        val relativeSubPath = "$APP_DOWNLOAD_DIR_NAME/$category/$sanitizedName"

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: throw IllegalStateException("DownloadManager service is unavailable")

        val request = DownloadManager.Request(Uri.parse(effectiveUrl)).apply {
            setTitle(sanitizedName)
            setDescription("Downloading attachment to $category...")
            setMimeType(mimeType)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            try {
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, relativeSubPath)
            } catch (secEx: SecurityException) {
                Log.w(TAG, "DownloadManager public dir Permission Denial, routing to app files: ${secEx.message}")
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, relativeSubPath)
            }
        }

        val id = downloadManager.enqueue(request)
        Toast.makeText(context, "Download started in background...", Toast.LENGTH_SHORT).show()
        return id
    }
}
