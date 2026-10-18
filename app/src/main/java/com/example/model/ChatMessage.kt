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
    val attachmentName: String? = null,
    val attachmentType: String? = null,       // e.g. "PDF", "DOC", "IMAGE", "SHEET", "ARCHIVE", "FILE"
    val attachmentSize: String? = null
) {
    val isFaculty: Boolean
        get() = senderRole.equals("TEACHER", ignoreCase = true)

    val hasAttachment: Boolean
        get() = !attachmentName.isNullOrBlank() || !attachmentUri.isNullOrBlank()
}
