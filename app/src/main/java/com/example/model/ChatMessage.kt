package com.example.model

/**
 * Message entry saved under /threads/{threadId}/messages/{messageId}.
 * All properties provide default values for Firestore deserialization.
 */
data class ChatMessage(
    val messageId: String = "",
    val threadId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: String = ""
)
