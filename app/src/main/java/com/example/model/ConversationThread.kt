package com.example.model

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
    val id: String = "",
    val name: String = "",
    val lastMessage: String = "",
    val timestamp: String = "",
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isVerified: Boolean = false,
    val isFaculty: Boolean = false,
    val scholasticBadge: String = "",
    val isGroup: Boolean = false,
    val category: ConversationCategory = ConversationCategory.DIRECT,
    val avatarInitials: String = "",
    val avatarColorStart: Long = 0xFF4F46E5,
    val avatarColorEnd: Long = 0xFF2563EB,
    val lastSenderName: String? = null,
    val lastSenderId: String? = null,
    val participantIds: List<String> = emptyList(),
    val messageStatus: MessageStatus = MessageStatus.READ,
    val snippetType: SnippetType = SnippetType.TEXT
)

data class ActiveContact(
    val id: String = "",
    val name: String = "",
    val initials: String = "",
    val isOnline: Boolean = false,
    val isFaculty: Boolean = false,
    val scholasticBadge: String = "",
    val hasStory: Boolean = false,
    val colorStart: Long = 0xFF4F46E5,
    val colorEnd: Long = 0xFF2563EB
)
