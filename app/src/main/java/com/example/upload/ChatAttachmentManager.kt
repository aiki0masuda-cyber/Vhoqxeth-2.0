package com.example.upload

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import okio.source
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// ==============================================================================
// 1. ATTACHMENT UPLOAD STATE
// ==============================================================================

/**
 * Sealed interface representing all states of the file attachment upload lifecycle.
 */
sealed interface AttachmentUploadState {
    data object Idle : AttachmentUploadState
    data object FetchingTicket : AttachmentUploadState
    data class Uploading(
        val progress: Float,
        val bytesWritten: Long = 0L,
        val totalBytes: Long = 0L
    ) : AttachmentUploadState {
        val percentage: Int get() = (progress * 100).toInt().coerceIn(0, 100)
    }
    data class Success(val url: String) : AttachmentUploadState
    data class Error(val msg: String) : AttachmentUploadState
}

// ==============================================================================
// 2. URI & FILE METADATA RESOLUTION UTILITY
// ==============================================================================

data class SelectedFileMetadata(
    val name: String,
    val extension: String,
    val sizeBytes: Long,
    val mimeType: String
)

object UriFileUtils {
    /**
     * Extracts proper file extension, display name, and size from an Android Content URI.
     */
    fun extractFileMetadata(contentResolver: ContentResolver, uri: Uri): SelectedFileMetadata {
        var displayName = "attachment_${System.currentTimeMillis()}"
        var sizeBytes = 0L

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        displayName = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("UriFileUtils", "Failed to query content resolver for metadata", e)
        }

        val rawMimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val extension = displayName.substringAfterLast('.', "").ifBlank {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(rawMimeType) ?: "bin"
        }

        return SelectedFileMetadata(
            name = displayName,
            extension = extension.lowercase(),
            sizeBytes = sizeBytes,
            mimeType = rawMimeType
        )
    }
}

// ==============================================================================
// 3. OKHTTP CHAT ATTACHMENT MANAGER
// ==============================================================================

/**
 * Handles two-step pre-signed uploads to a private Backblaze B2 bucket via Cloudflare Worker
 * and resolves authenticated temporary download URLs for private image/file rendering.
 */
class ChatAttachmentManager(
    private val workerBaseUrl: String = "https://b2-chat-gatekeeper.aiki0masuda.workers.dev/",
    private val client: OkHttpClient = defaultOkHttpClient()
) {
    companion object {
        private const val TAG = "ChatAttachmentManager"

        fun defaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    private val activeCalls = ConcurrentHashMap<String, Call>()

    /**
     * STEP 1: Requests a temporary pre-signed PUT upload URL from the Cloudflare Worker gatekeeper.
     * GET https://b2-chat-gatekeeper.aiki0masuda.workers.dev/?action=upload&filename={FILENAME}
     */
    suspend fun fetchUploadUrl(filename: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val encodedFilename = URLEncoder.encode(filename, "UTF-8")
            val cleanBase = workerBaseUrl.trimEnd('/')
            val targetUrl = if (cleanBase.contains("?")) {
                "$cleanBase&action=upload&filename=$encodedFilename"
            } else {
                "$cleanBase/?action=upload&filename=$encodedFilename"
            }

            Log.d(TAG, "Step 1: Requesting upload URL from: $targetUrl")

            val request = Request.Builder()
                .url(targetUrl)
                .get()
                .header("Accept", "application/json")
                .build()

            suspendCancellableCoroutine { continuation ->
                val call = client.newCall(request)
                continuation.invokeOnCancellation {
                    call.cancel()
                }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "OkHttp onFailure: ${e.localizedMessage}", e)
                        Log.e(TAG, "Failed to fetch pre-signed ticket from worker: ${e.localizedMessage}", e)
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(IOException("Upload ticket error: ${e.localizedMessage}", e)))
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use { res ->
                            try {
                                val body = res.body?.string().orEmpty()
                                if (!res.isSuccessful) {
                                    val errMsg = try {
                                        JSONObject(body).optString("error", "HTTP ${res.code}")
                                    } catch (_: Exception) {
                                        "HTTP ${res.code}: ${res.message}"
                                    }
                                    Log.e(TAG, "OkHttp onFailure (HTTP ${res.code}): $errMsg")
                                    if (continuation.isActive) {
                                        continuation.resume(Result.failure(IOException("Gatekeeper error: $errMsg")))
                                    }
                                    return
                                }

                                val json = JSONObject(body)
                                val uploadUrl = json.getString("uploadUrl")
                                Log.d(TAG, "Received pre-signed uploadUrl: $uploadUrl")
                                if (continuation.isActive) {
                                    continuation.resume(Result.success(uploadUrl))
                                }
                            } catch (ex: Exception) {
                                Log.e(TAG, "OkHttp onFailure parsing JSON: ${ex.localizedMessage}", ex)
                                if (continuation.isActive) {
                                    continuation.resume(Result.failure(IOException("Response parse error: ${ex.localizedMessage}", ex)))
                                }
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "OkHttp onFailure: ${e.localizedMessage}", e)
            Log.e(TAG, "Failed to fetch pre-signed ticket from worker: ${e.localizedMessage}", e)
            Result.failure(IOException("Upload ticket error: ${e.localizedMessage}", e))
        }
    }

    /**
     * STEP 2: Streams file binary directly to the pre-signed Backblaze B2 PUT URL
     * with real-time byte-level progress reporting and Content-Type: application/octet-stream.
     */
    suspend fun uploadFileBinary(
        uploadId: String = UUID.randomUUID().toString(),
        uploadUrl: String,
        contentResolver: ContentResolver,
        fileUri: Uri,
        totalSizeBytes: Long,
        onProgress: (progress: Float, bytesWritten: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val requestBody = CountingInputStreamRequestBody(
                contentLength = totalSizeBytes,
                openStream = {
                    contentResolver.openInputStream(fileUri)
                        ?: throw IOException("Cannot open input stream for: $fileUri")
                },
                onProgress = onProgress
            )

            val request = Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
                .header("Content-Type", "application/octet-stream")
                .build()

            val call = client.newCall(request)
            activeCalls[uploadId] = call

            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    call.cancel()
                    activeCalls.remove(uploadId)
                }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        activeCalls.remove(uploadId)
                        Log.e(TAG, "OkHttp onFailure streaming binary: ${e.localizedMessage}", e)
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(e))
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        activeCalls.remove(uploadId)
                        response.use { res ->
                            if (!res.isSuccessful) {
                                val body = res.body?.string().orEmpty()
                                Log.e(TAG, "OkHttp onFailure - S3 PUT status ${res.code}: $body")
                                if (continuation.isActive) {
                                    continuation.resume(
                                        Result.failure(IOException("S3 PUT failed with HTTP ${res.code}: $body"))
                                    )
                                }
                            } else {
                                Log.i(TAG, "File binary streamed successfully to Backblaze B2 (uploadId=$uploadId)")
                                if (continuation.isActive) {
                                    continuation.resume(Result.success(Unit))
                                }
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            activeCalls.remove(uploadId)
            Log.e(TAG, "OkHttp onFailure: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    /**
     * STEP 3: Private Bucket Download Link Resolver.
     * GET https://b2-chat-gatekeeper.aiki0masuda.workers.dev/?action=download&filename={FILENAME}
     * Retrieves an authenticated temporary download URL for safe media display.
     */
    suspend fun fetchSecureDownloadUrl(filename: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val encodedFilename = URLEncoder.encode(filename, "UTF-8")
            val cleanBase = workerBaseUrl.trimEnd('/')
            val targetUrl = if (cleanBase.contains("?")) {
                "$cleanBase&action=download&filename=$encodedFilename"
            } else {
                "$cleanBase/?action=download&filename=$encodedFilename"
            }

            Log.d(TAG, "Requesting secure download link: $targetUrl")

            val request = Request.Builder()
                .url(targetUrl)
                .get()
                .header("Accept", "application/json")
                .build()

            suspendCancellableCoroutine { continuation ->
                val call = client.newCall(request)
                continuation.invokeOnCancellation {
                    call.cancel()
                }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "OkHttp onFailure: ${e.localizedMessage}", e)
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(IOException("Download link error: ${e.localizedMessage}", e)))
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use { res ->
                            try {
                                val body = res.body?.string().orEmpty()
                                if (!res.isSuccessful) {
                                    val errMsg = try {
                                        JSONObject(body).optString("error", "HTTP ${res.code}")
                                    } catch (_: Exception) {
                                        "HTTP ${res.code}: ${res.message}"
                                    }
                                    Log.e(TAG, "OkHttp onFailure for download URL: $errMsg")
                                    if (continuation.isActive) {
                                        continuation.resume(Result.failure(IOException("Failed to get download URL: $errMsg")))
                                    }
                                    return
                                }

                                val json = JSONObject(body)
                                val downloadUrl = json.optString("downloadUrl").ifBlank {
                                    json.optString("temporaryDownloadUrl")
                                }

                                if (downloadUrl.isBlank()) {
                                    if (continuation.isActive) {
                                        continuation.resume(Result.failure(IOException("Missing 'downloadUrl' in gatekeeper response")))
                                    }
                                } else {
                                    if (continuation.isActive) {
                                        continuation.resume(Result.success(downloadUrl))
                                    }
                                }
                            } catch (ex: Exception) {
                                Log.e(TAG, "OkHttp onFailure parsing download JSON: ${ex.localizedMessage}", ex)
                                if (continuation.isActive) {
                                    continuation.resume(Result.failure(IOException("Download link JSON error: ${ex.localizedMessage}", ex)))
                                }
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "OkHttp onFailure: ${e.localizedMessage}", e)
            Result.failure(IOException("Download link error: ${e.localizedMessage}", e))
        }
    }

    fun cancelUpload(uploadId: String) {
        activeCalls.remove(uploadId)?.cancel()
    }
}

/**
 * Custom OkHttp RequestBody that streams from an InputStream without reading the entire
 * file into memory, emitting exact progress updates via an Okio ForwardingSink.
 */
internal class CountingInputStreamRequestBody(
    private val contentLength: Long,
    private val openStream: () -> InputStream,
    private val onProgress: (progress: Float, bytesWritten: Long, totalBytes: Long) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = "application/octet-stream".toMediaTypeOrNull()

    override fun contentLength(): Long = contentLength

    override fun writeTo(sink: BufferedSink) {
        val countingSink = object : ForwardingSink(sink) {
            private var bytesWritten = 0L
            private var lastProgressPercent = -1

            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                bytesWritten += byteCount
                val progress = if (contentLength > 0L) {
                    (bytesWritten.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                } else 0f

                val currentPercent = (progress * 100).toInt()
                if (currentPercent != lastProgressPercent || bytesWritten == contentLength) {
                    lastProgressPercent = currentPercent
                    onProgress(progress, bytesWritten, contentLength)
                }
            }
        }

        val bufferedSink = countingSink.buffer()
        openStream().use { inputStream ->
            val source = inputStream.source()
            bufferedSink.writeAll(source)
            bufferedSink.flush()
        }
    }
}

// ==============================================================================
// 4. VIEWMODEL STATE ARCHITECTURE
// ==============================================================================

data class ChatMessageItem(
    val messageId: String,
    val senderName: String,
    val isMe: Boolean,
    val text: String,
    val fileName: String? = null,
    val mediaUrl: String? = null,
    val isUploadingPlaceholder: Boolean = false,
    val uploadProgress: Float = 0f
)

class SampleChatViewModel(
    private val attachmentManager: ChatAttachmentManager = ChatAttachmentManager(),
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessageItem>>(emptyList())
    val messages: StateFlow<List<ChatMessageItem>> = _messages.asStateFlow()

    private val _uploadState = MutableStateFlow<AttachmentUploadState>(AttachmentUploadState.Idle)
    val uploadState: StateFlow<AttachmentUploadState> = _uploadState.asStateFlow()

    /**
     * Executes the complete attachment workflow:
     * 1. Inserts an uploading placeholder card into the chat list.
     * 2. Obtains a pre-signed PUT upload URL from Cloudflare Worker.
     * 3. Streams raw binary directly to Backblaze B2 with real-time progress.
     * 4. Resolves the authenticated private download URL.
     * 5. Dynamically swaps the placeholder card for the live media view.
     */
    fun sendAttachment(fileUri: Uri, caption: String = "") {
        val metadata = UriFileUtils.extractFileMetadata(contentResolver, fileUri)
        val tempId = "upload_${System.currentTimeMillis()}"

        // Step 1: Instantly insert uploading placeholder card
        val placeholder = ChatMessageItem(
            messageId = tempId,
            senderName = "You",
            isMe = true,
            text = caption,
            fileName = metadata.name,
            isUploadingPlaceholder = true,
            uploadProgress = 0f
        )
        _messages.value = _messages.value + placeholder
        _uploadState.value = AttachmentUploadState.FetchingTicket

        viewModelScope.launch {
            // STEP 1: Request upload URL
            val uploadTicketResult = attachmentManager.fetchUploadUrl(metadata.name)
            if (uploadTicketResult.isFailure) {
                val errorMsg = uploadTicketResult.exceptionOrNull()?.message ?: "Failed to get upload authorization"
                _uploadState.value = AttachmentUploadState.Error(errorMsg)
                _messages.value = _messages.value.filterNot { it.messageId == tempId }
                return@launch
            }

            val uploadUrl = uploadTicketResult.getOrThrow()
            _uploadState.value = AttachmentUploadState.Uploading(progress = 0f, totalBytes = metadata.sizeBytes)

            // STEP 2: Stream binary to Backblaze B2
            val streamResult = attachmentManager.uploadFileBinary(
                uploadId = tempId,
                uploadUrl = uploadUrl,
                contentResolver = contentResolver,
                fileUri = fileUri,
                totalSizeBytes = metadata.sizeBytes,
                onProgress = { progress, written, total ->
                    _uploadState.value = AttachmentUploadState.Uploading(progress, written, total)
                    _messages.value = _messages.value.map { msg ->
                        if (msg.messageId == tempId) msg.copy(uploadProgress = progress) else msg
                    }
                }
            )

            if (streamResult.isFailure) {
                val errorMsg = streamResult.exceptionOrNull()?.message ?: "Upload streaming failed"
                _uploadState.value = AttachmentUploadState.Error(errorMsg)
                _messages.value = _messages.value.filterNot { it.messageId == tempId }
                return@launch
            }

            // STEP 3: Resolve secure authenticated download link
            val secureDownloadResult = attachmentManager.fetchSecureDownloadUrl(metadata.name)
            val finalDownloadUrl = secureDownloadResult.getOrNull() ?: uploadUrl.substringBefore("?")

            _uploadState.value = AttachmentUploadState.Success(finalDownloadUrl)

            // Step 4: Dynamically swap placeholder card with active media view
            _messages.value = _messages.value.map { msg ->
                if (msg.messageId == tempId) {
                    msg.copy(
                        isUploadingPlaceholder = false,
                        uploadProgress = 1f,
                        mediaUrl = finalDownloadUrl
                    )
                } else msg
            }
        }
    }

    fun cancelActiveUpload(messageId: String) {
        attachmentManager.cancelUpload(messageId)
        _messages.value = _messages.value.filterNot { it.messageId == messageId }
        _uploadState.value = AttachmentUploadState.Idle
    }
}

// ==============================================================================
// 5. JETPACK COMPOSE UI INTEGRATION COMPONENTS
// ==============================================================================

/**
 * COMPOSABLE FILE PICKER TRIGGER:
 * Uses rememberLauncherForActivityResult with ActivityResultContracts.GetContent()
 * to pick any file or media attachment from device storage.
 */
@Composable
fun AttachmentFilePickerButton(
    onFileSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onFileSelected(it) }
    }

    IconButton(
        onClick = { filePickerLauncher.launch("*/*") },
        modifier = modifier.testTag("attachment_picker_button")
    ) {
        Icon(
            imageVector = Icons.Default.AttachFile,
            contentDescription = "Attach file",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Uploading placeholder card with visible linear progress bar, percentage, and cancel button.
 */
@Composable
fun UploadingPlaceholderCard(
    message: ChatMessageItem,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
            .fillMaxWidth(0.85f)
            .padding(vertical = 4.dp)
            .testTag("uploading_placeholder_${message.messageId}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = message.fileName ?: "Uploading attachment...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Uploading ${(message.uploadProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(28.dp).testTag("cancel_upload_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel upload",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { message.uploadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFFE2E8F0)
            )
        }
    }
}

/**
 * Active media message bubble displaying the confirmed image via Coil or file card.
 */
@Composable
fun ActiveMediaMessageCard(
    message: ChatMessageItem,
    modifier: Modifier = Modifier
) {
    val isImage = message.fileName?.matches(Regex(".*\\.(jpg|jpeg|png|webp|gif)$", RegexOption.IGNORE_CASE)) == true
            || (message.mediaUrl?.contains(Regex("\\.(jpg|jpeg|png|webp|gif)", RegexOption.IGNORE_CASE)) == true)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (message.isMe) MaterialTheme.colorScheme.primary else Color.White,
        border = if (message.isMe) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth(0.85f)
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (isImage && !message.mediaUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = message.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Secure private bucket lock badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Private B2",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "B2 SECURE",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = if (message.isMe) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message.fileName ?: "Document",
                        color = if (message.isMe) Color.White else Color(0xFF1E293B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (message.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    color = if (message.isMe) Color.White else Color(0xFF1E293B),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
