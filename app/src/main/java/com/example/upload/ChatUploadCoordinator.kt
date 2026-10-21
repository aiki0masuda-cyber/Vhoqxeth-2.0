package com.example.upload

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.MeshChatRepository
import com.example.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Conceptual UI State Change Helper coordinating in-flight uploads and optimistic UI rendering.
 *
 * Temporarily maintains a reactive list of [PendingUploadMessage] items for the current chat thread.
 * Upon successful completion via [S3UploadManager], it persists the final [ChatMessage] to the chat
 * database (e.g. Firebase Firestore) and clears the placeholder.
 */
class ChatUploadCoordinator(
    private val uploadManager: S3UploadManager = S3UploadManager(),
    private val gatekeeperUrl: String = S3UploadManager.DEFAULT_GATEKEEPER_ENDPOINT
) {
    companion object {
        private const val TAG = "ChatUploadCoordinator"
    }

    private val _pendingUploads = MutableStateFlow<List<PendingUploadMessage>>(emptyList())
    val pendingUploads: StateFlow<List<PendingUploadMessage>> = _pendingUploads.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()

    /**
     * Dispatches a new file attachment upload:
     * 1. Inserts a [PendingUploadMessage] placeholder into the UI state immediately.
     * 2. Launches the [S3UploadManager] background pipeline.
     * 3. Continuously streams progress to the placeholder bubble.
     * 4. Upon success, commits the record to Firestore via [chatRepository] and removes the placeholder.
     */
    fun enqueueUpload(
        context: Context,
        scope: CoroutineScope,
        threadId: String,
        currentUser: UserProfile,
        fileUri: Uri,
        fileName: String,
        fileType: String,
        fileSizeFormatted: String,
        caption: String,
        chatRepository: MeshChatRepository
    ): String {
        val uploadId = "upload_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        val initialPending = PendingUploadMessage(
            uploadId = uploadId,
            threadId = threadId,
            senderId = currentUser.customUserId,
            senderName = currentUser.displayName,
            localUri = fileUri,
            fileName = fileName,
            fileType = fileType,
            fileSizeFormatted = fileSizeFormatted,
            caption = caption,
            uploadState = UploadState.RequestingUrl
        )

        // Optimistically add to UI list
        _pendingUploads.update { it + initialPending }

        startUploadJob(
            uploadId = uploadId,
            context = context,
            scope = scope,
            threadId = threadId,
            currentUser = currentUser,
            fileUri = fileUri,
            fileName = fileName,
            fileType = fileType,
            fileSizeFormatted = fileSizeFormatted,
            caption = caption,
            chatRepository = chatRepository
        )

        return uploadId
    }

    /**
     * Retries a previously failed upload.
     */
    fun retryUpload(
        uploadId: String,
        context: Context,
        scope: CoroutineScope,
        currentUser: UserProfile,
        chatRepository: MeshChatRepository
    ) {
        val pending = _pendingUploads.value.firstOrNull { it.uploadId == uploadId } ?: return
        updateItemState(uploadId, UploadState.RequestingUrl)

        startUploadJob(
            uploadId = pending.uploadId,
            context = context,
            scope = scope,
            threadId = pending.threadId,
            currentUser = currentUser,
            fileUri = pending.localUri,
            fileName = pending.fileName,
            fileType = pending.fileType,
            fileSizeFormatted = pending.fileSizeFormatted,
            caption = pending.caption,
            chatRepository = chatRepository
        )
    }

    /**
     * Cancels an ongoing upload and removes it from the UI.
     */
    fun cancelUpload(uploadId: String) {
        uploadManager.cancelUpload(uploadId)
        activeJobs.remove(uploadId)?.cancel()
        _pendingUploads.update { list -> list.filterNot { it.uploadId == uploadId } }
        Log.d(TAG, "Dismissed pending upload $uploadId")
    }

    private fun startUploadJob(
        uploadId: String,
        context: Context,
        scope: CoroutineScope,
        threadId: String,
        currentUser: UserProfile,
        fileUri: Uri,
        fileName: String,
        fileType: String,
        fileSizeFormatted: String,
        caption: String,
        chatRepository: MeshChatRepository
    ) {
        val job = scope.launch(Dispatchers.IO) {
            val mimeType = resolveMimeType(fileType)

            // Step 1: Request pre-signed URL from gatekeeper
            val presignResult = uploadManager.requestPresignedUrl(
                gatekeeperUrl = gatekeeperUrl,
                fileName = fileName,
                contentType = mimeType,
                fileSizeBytes = -1L
            )

            if (presignResult.isFailure) {
                val error = presignResult.exceptionOrNull()?.message ?: "Failed to get pre-signed upload URL"
                updateItemState(uploadId, UploadState.Error(error, canRetry = true))
                return@launch
            }

            val presignResponse = presignResult.getOrThrow()

            // Step 2: Stream binary directly to Cloudflare R2
            updateItemState(uploadId, UploadState.Uploading(progress = 0.05f))

            val uploadResult = uploadManager.uploadBinaryDirectly(
                uploadId = uploadId,
                uploadUrl = presignResponse.uploadUrl,
                context = context,
                fileUri = fileUri,
                contentType = mimeType,
                onProgress = { progress, written, total ->
                    updateItemState(
                        uploadId,
                        UploadState.Uploading(
                            progress = progress,
                            bytesWritten = written,
                            totalBytes = total
                        )
                    )
                }
            )

            if (uploadResult.isFailure) {
                val error = uploadResult.exceptionOrNull()?.message ?: "Cloudflare R2 streaming failed"
                updateItemState(uploadId, UploadState.Error(error, canRetry = true))
                return@launch
            }

            // Step 3: Success! Commit the persistent ChatMessage to Firestore database
            updateItemState(uploadId, UploadState.Success(presignResponse.downloadUrl))

            try {
                chatRepository.sendFileMessage(
                    threadId = threadId,
                    senderProfile = currentUser,
                    content = caption,
                    attachmentUri = presignResponse.downloadUrl,
                    attachmentName = fileName,
                    attachmentType = fileType,
                    attachmentSize = fileSizeFormatted
                )
                // Remove placeholder once committed to database
                _pendingUploads.update { list -> list.filterNot { it.uploadId == uploadId } }
                Log.i(TAG, "Committed upload $uploadId to chat database with URL: ${presignResponse.downloadUrl}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to commit message to Firestore: ${e.message}", e)
                updateItemState(uploadId, UploadState.Error("Saved to R2 but failed to post in chat: ${e.message}"))
            }
        }

        activeJobs[uploadId] = job
    }

    private fun updateItemState(uploadId: String, newState: UploadState) {
        _pendingUploads.update { list ->
            list.map { item ->
                if (item.uploadId == uploadId) item.copy(uploadState = newState) else item
            }
        }
    }

    private fun resolveMimeType(fileType: String): String {
        return when (fileType.uppercase()) {
            "IMAGE" -> "image/jpeg"
            "PDF" -> "application/pdf"
            "VIDEO" -> "video/mp4"
            else -> "application/octet-stream"
        }
    }
}
