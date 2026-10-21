package com.example.upload

import android.net.Uri

/**
 * Optimistic representation of a message whose media attachment is currently
 * uploading to Cloudflare R2 via pre-signed URL.
 *
 * Rendered in the Composable chat stream *before* committing the finalized
 * [ChatMessage] to Firestore or websocket database.
 */
data class PendingUploadMessage(
    val uploadId: String,
    val threadId: String,
    val senderId: String,
    val senderName: String,
    val localUri: Uri,
    val fileName: String,
    val fileType: String, // e.g. "IMAGE", "PDF", "VIDEO", "FILE"
    val fileSizeFormatted: String,
    val caption: String = "",
    val uploadState: UploadState = UploadState.Idle,
    val timestamp: String = "Uploading..."
) {
    val isError: Boolean
        get() = uploadState is UploadState.Error

    val isUploading: Boolean
        get() = uploadState is UploadState.Uploading || uploadState is UploadState.RequestingUrl
}
