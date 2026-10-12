package com.example.model

import androidx.compose.ui.graphics.Color

enum class MessageStatus {
    NONE,
    SENT,
    DELIVERED,
    READ
}

enum class ConversationCategory {
    ALL,
    DIRECT,
    GROUP,
    CHANNEL,
    UNREAD
}

enum class SnippetType {
    TEXT,
    AUDIO,
    IMAGE,
    FILE,
    SYSTEM
}

data class ConversationThread(
    val id: String,
    val name: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isVerified: Boolean = false,
    val isGroup: Boolean = false,
    val category: ConversationCategory = ConversationCategory.DIRECT,
    val avatarInitials: String,
    val avatarColorStart: Long,
    val avatarColorEnd: Long,
    val lastSenderName: String? = null,
    val messageStatus: MessageStatus = MessageStatus.READ,
    val snippetType: SnippetType = SnippetType.TEXT
)

data class ActiveContact(
    val id: String,
    val name: String,
    val initials: String,
    val isOnline: Boolean,
    val hasStory: Boolean,
    val colorStart: Long,
    val colorEnd: Long
)
