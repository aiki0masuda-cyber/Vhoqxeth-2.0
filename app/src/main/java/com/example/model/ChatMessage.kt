package com.example.model

/**
 * Message entry saved under /threads/{threadId}/messages/{messageId}.
 * Extended with universal file attachment support.
 * All properties provide default values for Firestore deserialization.
 */
data class ChatMessage(
    val messageId: String = "",
    val threadId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "STUDENT",       // "STUDENT" or "TEACHER"
    val content: String = "",
    val timestamp: String = "",
    val attachmentUri: String? = null,
    val attachmentUrl: String? = null,        // S3 / B2 download URL
    val temporaryDownloadUrl: String? = null, // Active pre-signed 24-hour GET URL for private bucket
    val attachmentKey: String? = null,        // S3 object key (e.g. chat-attachments/1713681200000_sample.png)
    val attachmentName: String? = null,
    val attachmentType: String? = null,       // "IMAGE", "PDF", "VIDEO", "DOC", "FILE"
    val attachmentSize: String? = null,
    val isUploadingPlaceholder: Boolean = false, // True while streaming binary to pre-signed URL
    val uploadProgress: Float = 0f,           // 0.0f to 1.0f progress for active placeholder
    val voiceDurationSeconds: Int = 0         // Duration in seconds if audio voice message
) {
    val isFaculty: Boolean
        get() = senderRole.equals("TEACHER", ignoreCase = true)

    val isVoiceMessage: Boolean
        get() = attachmentType.equals("VOICE", ignoreCase = true) ||
                (attachmentName?.endsWith(".m4a", ignoreCase = true) == true) ||
                (attachmentName?.endsWith(".aac", ignoreCase = true) == true)

    val resolvedAttachmentUrl: String?
        get() = temporaryDownloadUrl?.ifBlank { null }
            ?: attachmentUrl?.ifBlank { null }
            ?: attachmentUri?.ifBlank { null }

    val hasAttachment: Boolean
        get() = !attachmentName.isNullOrBlank() || !resolvedAttachmentUrl.isNullOrBlank()

    val isS3SignedUrl: Boolean
        get() = resolvedAttachmentUrl?.contains("X-Amz-Signature") == true

    fun isAttachmentExpired(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        if (!hasAttachment) return false
        if (attachmentKey.isNullOrBlank()) return false
        val url = resolvedAttachmentUrl ?: return true
        return !url.contains("X-Amz-Signature")
    }
}
