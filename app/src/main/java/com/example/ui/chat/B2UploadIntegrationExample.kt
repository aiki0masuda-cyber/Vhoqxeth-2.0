package com.example.ui.chat

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.B2UploadManager
import com.example.model.ChatMessage
import com.example.model.UploadState
import com.example.model.UserProfile
import com.example.ui.theme.DavAccentSky
import com.example.ui.theme.DavBrandRoyalBlue
import com.example.ui.theme.DavTextPrimary
import com.example.ui.theme.DavTextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Section 4 Reference: ViewModel Workflow showing how to immediately push an
 * optimistic placeholder bubble into the chat message list, stream binary to Backblaze B2,
 * track progress smoothly, and finalize the message with publicDownloadUrl.
 */
class ChatAttachmentViewModel(
    private val uploadManager: B2UploadManager = B2UploadManager(),
    private val workerEndpoint: String = B2UploadManager.DEFAULT_WORKER_ENDPOINT
) : ViewModel() {

    // Message stream: holds confirmed Firestore messages + active optimistic placeholders
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Active upload states keyed by temporary messageId
    private val _uploadStates = MutableStateFlow<Map<String, UploadState>>(emptyMap())
    val uploadStates: StateFlow<Map<String, UploadState>> = _uploadStates.asStateFlow()

    /**
     * Executes the optimistic upload workflow:
     * 1. Creates an in-memory placeholder with isUploadingPlaceholder = true
     * 2. Immediately appends it to the local UI state
     * 3. Calls B2UploadManager.executeCompleteUpload
     * 4. Updates progress reactively
     * 5. On success, replaces the placeholder with the permanent downloadUrl
     */
    fun sendAttachment(
        threadId: String,
        currentUser: UserProfile,
        content: String,
        contentResolver: ContentResolver,
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        fileSizeBytes: Long,
        onCommitToDatabase: suspend (threadId: String, message: ChatMessage) -> Unit
    ) {
        val tempMessageId = "pending_${UUID.randomUUID().toString().take(8)}"
        val attachmentType = when {
            mimeType.startsWith("image/") -> "IMAGE"
            mimeType.startsWith("video/") -> "VIDEO"
            mimeType.contains("pdf") -> "PDF"
            else -> "FILE"
        }

        // Step 1 & 2: Push optimistic placeholder to UI
        val placeholder = ChatMessage(
            messageId = tempMessageId,
            threadId = threadId,
            senderId = currentUser.customUserId,
            senderName = currentUser.displayName,
            content = content,
            timestamp = "Uploading...",
            attachmentUri = fileUri.toString(),
            attachmentName = fileName,
            attachmentType = attachmentType,
            attachmentSize = formatFileSize(fileSizeBytes),
            isUploadingPlaceholder = true,
            uploadProgress = 0f
        )

        _messages.value = _messages.value + placeholder

        // Step 3: Launch asynchronous streaming to Backblaze B2 via pre-signed URL
        viewModelScope.launch {
            val uploadResult = uploadManager.executeCompleteUploadWithKey(
                uploadId = tempMessageId,
                workerEndpoint = workerEndpoint,
                contentResolver = contentResolver,
                fileUri = fileUri,
                filename = fileName,
                contentType = mimeType,
                fileSizeBytes = fileSizeBytes,
                onStateChanged = { state ->
                    _uploadStates.value = _uploadStates.value + (tempMessageId to state)

                    // Update progress in the placeholder message
                    if (state is UploadState.Uploading) {
                        _messages.value = _messages.value.map { msg ->
                            if (msg.messageId == tempMessageId) {
                                msg.copy(uploadProgress = state.progress)
                            } else msg
                        }
                    }
                }
            )

            // Step 4: Handle result
            if (uploadResult.isSuccess) {
                val completion = uploadResult.getOrThrow()

                // Finalize the message: remove placeholder flag and attach 24h temporary URL & storage key
                val finalizedMessage = placeholder.copy(
                    isUploadingPlaceholder = false,
                    uploadProgress = 1f,
                    attachmentUri = completion.downloadUrl,
                    attachmentUrl = completion.downloadUrl,
                    temporaryDownloadUrl = completion.downloadUrl,
                    attachmentKey = completion.key
                )

                // Update UI list
                _messages.value = _messages.value.map { msg ->
                    if (msg.messageId == tempMessageId) finalizedMessage else msg
                }

                // Step 5: Commit to remote database (e.g. Firebase Firestore)
                onCommitToDatabase(threadId, finalizedMessage)
            } else {
                // Keep the placeholder in error state so the user can retry or dismiss
                val errorMsg = uploadResult.exceptionOrNull()?.message ?: "Upload failed"
                _uploadStates.value = _uploadStates.value + (tempMessageId to UploadState.Error(errorMsg))
            }
        }
    }

    fun cancelUpload(tempMessageId: String) {
        uploadManager.cancelUpload(tempMessageId)
        _messages.value = _messages.value.filterNot { it.messageId == tempMessageId }
        _uploadStates.value = _uploadStates.value - tempMessageId
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }
}

/**
 * Section 4 Reference: Composable UI component that renders an uploading placeholder card
 * with a visible progress indicator, live percentage, file type icon, and Cancel/Retry controls.
 */
@Composable
fun UploadingPlaceholderBubble(
    message: ChatMessage,
    uploadState: UploadState?,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = message.uploadProgress.coerceIn(0f, 1f),
        label = "upload_progress_anim"
    )

    val isError = uploadState is UploadState.Error
    val isFetchingTicket = uploadState is UploadState.FetchingTicket

    Box(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .border(
                width = 1.dp,
                color = if (isError) Color(0xFFEF4444) else DavAccentSky.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
            .testTag("uploading_placeholder_bubble_${message.messageId}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Icon + Filename + Cancel button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isError) Color(0xFFFEE2E2) else DavBrandRoyalBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (message.attachmentType) {
                            "IMAGE" -> Icons.Default.Image
                            "VIDEO" -> Icons.Default.Videocam
                            "PDF" -> Icons.Default.Description
                            else -> Icons.Default.CloudUpload
                        },
                        contentDescription = message.attachmentType,
                        tint = if (isError) Color(0xFFDC2626) else DavBrandRoyalBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.attachmentName ?: "Attachment",
                        color = DavTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isError) {
                            (uploadState as UploadState.Error).message
                        } else if (isFetchingTicket) {
                            "Requesting B2 authorization..."
                        } else {
                            "${(message.uploadProgress * 100).toInt()}% • Streaming to Backblaze B2"
                        },
                        color = if (isError) Color(0xFFDC2626) else DavTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel upload",
                        tint = DavTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Progress Bar / Indicator
            if (!isError) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = DavBrandRoyalBlue,
                    trackColor = Color(0xFFE2E8F0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            } else {
                // Retry action button
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEE2E2))
                            .clickable(onClick = onRetry)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Retry Upload",
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Optional message caption
            if (message.content.isNotBlank()) {
                Text(
                    text = message.content,
                    color = DavTextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
