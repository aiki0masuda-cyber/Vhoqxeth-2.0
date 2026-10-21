package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.ChatMessage
import com.example.model.ConversationCategory
import com.example.model.ConversationThread
import com.example.model.MessageStatus
import com.example.model.SnippetType
import com.example.model.UserContact
import com.example.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MeshChatRepository(
    private val db: FirebaseFirestore
) {
    private val tag = "MeshChatRepository"

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun getCurrentIsoTimestamp(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return format.format(Date())
    }

    private fun formatRelativeTime(isoString: String): String {
        if (isoString.isBlank()) return "Now"
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val date = format.parse(isoString) ?: return "Just now"
            val diffMs = System.currentTimeMillis() - date.time
            val diffMinutes = diffMs / 60000
            val diffHours = diffMinutes / 60
            val diffDays = diffHours / 24

            when {
                diffMinutes < 1 -> "Now"
                diffMinutes < 60 -> "${diffMinutes}m"
                diffHours < 24 -> "${diffHours}h"
                diffDays < 7 -> "${diffDays}d"
                else -> SimpleDateFormat("MMM d", Locale.US).format(date)
            }
        } catch (_: Exception) {
            "Active"
        }
    }

    /**
     * Generates a deterministic thread ID between two participant user handles.
     */
    fun generateThreadId(user1: String, user2: String): String {
        val u1 = user1.trim().removePrefix("@").lowercase()
        val u2 = user2.trim().removePrefix("@").lowercase()
        val sorted = listOf(u1, u2).sorted()
        return "thread_${sorted[0]}_${sorted[1]}"
    }

    /**
     * Real-time flow of user contacts from /users/{userId}/contacts
     */
    fun observeContacts(userId: String): Flow<List<UserContact>> = callbackFlow {
        val sanitized = userId.trim().removePrefix("@")
        if (sanitized.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = db.collection("users")
            .document(sanitized)
            .collection("contacts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to contacts for $sanitized: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val contacts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserContact::class.java)
                    }
                    trySend(contacts)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            registration.remove()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Real-time flow of shared threads where current user is a participant.
     */
    fun observeThreads(userId: String): Flow<List<ConversationThread>> = callbackFlow {
        val sanitized = userId.trim().removePrefix("@").lowercase()
        if (sanitized.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = db.collection("threads")
            .whereArrayContains("participantIds", sanitized)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to threads for $sanitized: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val threads = snapshot.documents.mapNotNull { doc ->
                        val threadId = doc.getString("threadId") ?: doc.id
                        @Suppress("UNCHECKED_CAST")
                        val participants = (doc.get("participantIds") as? List<String>) ?: emptyList()
                        val lastMessage = doc.getString("lastMessage") ?: ""
                        val lastSenderId = doc.getString("lastSenderId") ?: ""
                        val lastSenderName = doc.getString("lastSenderName") ?: ""
                        val updatedAt = doc.getString("updatedAt") ?: ""

                        val isGroup = doc.getBoolean("isGroup") == true || doc.getString("category") == "GROUP"
                        val groupName = doc.getString("groupName")
                        val rawCategory = doc.getString("category") ?: if (isGroup) "GROUP" else "DIRECT"
                        val category = when (rawCategory) {
                            "GROUP" -> ConversationCategory.GROUP
                            "CHANNEL" -> ConversationCategory.CHANNEL
                            else -> ConversationCategory.DIRECT
                        }
                        val creatorId = doc.getString("creatorId") ?: ""
                        @Suppress("UNCHECKED_CAST")
                        val rawAdminIds = (doc.get("adminIds") as? List<String>) ?: emptyList()
                        val adminIds = if (rawAdminIds.isEmpty() && creatorId.isNotBlank()) listOf(creatorId) else rawAdminIds
                        val onlyAdminsCanChat = doc.getBoolean("onlyAdminsCanChat") == true
                        val onlyAdminsCanAddMembers = doc.getBoolean("onlyAdminsCanAddMembers") == true
                        val avatarUri = doc.getString("avatarUri")
                        val subject = doc.getString("subject") ?: ""
                        val grade = doc.getString("grade") ?: ""
                        val section = doc.getString("section") ?: ""
                        val description = doc.getString("description") ?: ""

                        // Determine display name and initials
                        val displayName: String
                        val initials: String
                        val colorStart: Long
                        val colorEnd: Long

                        if (isGroup && !groupName.isNullOrBlank()) {
                            displayName = groupName
                            initials = groupName.take(2).uppercase().ifBlank { "SC" }
                            colorStart = 0xFF0284C7
                            colorEnd = 0xFF38BDF8
                        } else {
                            val peerHandle = participants.firstOrNull { it != sanitized } ?: sanitized
                            displayName = peerHandle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                            initials = displayName.take(2).uppercase().ifBlank { "DC" }
                            colorStart = 0xFF4F46E5
                            colorEnd = 0xFF2563EB
                        }

                        ConversationThread(
                            id = threadId,
                            name = displayName,
                            lastMessage = lastMessage,
                            timestamp = formatRelativeTime(updatedAt),
                            unreadCount = 0,
                            isOnline = true,
                            isPinned = false,
                            isMuted = false,
                            isVerified = true,
                            isGroup = isGroup,
                            category = category,
                            avatarInitials = initials,
                            avatarColorStart = colorStart,
                            avatarColorEnd = colorEnd,
                            lastSenderName = lastSenderName,
                            lastSenderId = lastSenderId,
                            participantIds = participants,
                            messageStatus = MessageStatus.DELIVERED,
                            snippetType = SnippetType.TEXT,
                            creatorId = creatorId,
                            adminIds = adminIds,
                            onlyAdminsCanChat = onlyAdminsCanChat,
                            onlyAdminsCanAddMembers = onlyAdminsCanAddMembers,
                            avatarUri = avatarUri,
                            subject = subject,
                            grade = grade,
                            section = section,
                            description = description
                        )
                    }.sortedByDescending { it.timestamp }

                    trySend(threads)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            registration.remove()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Real-time flow of messages in a thread from /threads/{threadId}/messages
     */
    fun observeMessages(threadId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (threadId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = db.collection("threads")
            .document(threadId)
            .collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to messages in $threadId: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)
                    }.sortedBy { it.timestamp }
                    trySend(messages)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            registration.remove()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Search for a registered user profile by handle.
     */
    suspend fun searchUserByHandle(handle: String): Result<UserProfile?> = withContext(Dispatchers.IO) {
        try {
            val key = handle.trim().removePrefix("@")
            if (key.isBlank()) return@withContext Result.success(null)

            val doc = db.collection("users").document(key).get().await()
            if (doc.exists()) {
                val profile = doc.toObject(UserProfile::class.java)
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(tag, "Search user failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Adds a user contact to the current user's roster, adds reciprocal contact,
     * and initializes the shared thread in Firestore.
     */
    suspend fun addContact(
        currentUser: UserProfile,
        targetUser: UserProfile
    ): Result<ConversationThread> = withContext(Dispatchers.IO) {
        try {
            val currentHandle = currentUser.customUserId.trim().removePrefix("@").lowercase()
            val targetHandle = targetUser.customUserId.trim().removePrefix("@").lowercase()

            if (currentHandle.isBlank() || targetHandle.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("User handle cannot be blank."))
            }

            if (currentHandle == targetHandle) {
                return@withContext Result.failure(IllegalArgumentException("Cannot add your own User ID as a contact."))
            }

            val now = getCurrentIsoTimestamp()

            // 1. Save to current user's contacts
            val contactPayloadCurrent = mapOf(
                "contactUserId" to targetHandle,
                "displayName" to targetUser.displayName.ifBlank { targetHandle },
                "currentIp" to targetUser.currentIp,
                "addedAt" to now
            )
            try {
                db.collection("users")
                    .document(currentHandle)
                    .collection("contacts")
                    .document(targetHandle)
                    .set(contactPayloadCurrent)
                    .await()
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Failed Step 1 (users/$currentHandle/contacts/$targetHandle): ${e.message}", e))
            }

            // 2. Save reciprocal contact in target user's contacts
            val contactPayloadTarget = mapOf(
                "contactUserId" to currentHandle,
                "displayName" to currentUser.displayName.ifBlank { currentHandle },
                "currentIp" to currentUser.currentIp,
                "addedAt" to now
            )
            try {
                db.collection("users")
                    .document(targetHandle)
                    .collection("contacts")
                    .document(currentHandle)
                    .set(contactPayloadTarget)
                    .await()
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Failed Step 2 (users/$targetHandle/contacts/$currentHandle): ${e.message}", e))
            }

            // 3. Ensure deterministic shared thread exists
            val threadId = generateThreadId(currentHandle, targetHandle)
            val threadDoc = db.collection("threads").document(threadId).get().await()

            val initialMessage = "Encrypted mesh session established"
            if (!threadDoc.exists()) {
                val threadPayload = mapOf(
                    "threadId" to threadId,
                    "participantIds" to listOf(currentHandle, targetHandle).sorted(),
                    "lastMessage" to initialMessage,
                    "lastSenderId" to currentHandle,
                    "lastSenderName" to currentUser.displayName,
                    "updatedAt" to now
                )
                try {
                    db.collection("threads").document(threadId).set(threadPayload).await()
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("Failed Step 3 (threads/$threadId): ${e.message}", e))
                }

                // Add welcome handshake message
                val msgId = "msg_${UUID.randomUUID().toString().take(8)}"
                val msgPayload = mapOf(
                    "messageId" to msgId,
                    "threadId" to threadId,
                    "senderId" to currentHandle,
                    "senderName" to currentUser.displayName,
                    "content" to initialMessage,
                    "timestamp" to now
                )
                try {
                    db.collection("threads").document(threadId).collection("messages").document(msgId).set(msgPayload).await()
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("Failed Step 4 (threads/$threadId/messages/$msgId): ${e.message}", e))
                }
            }

            val resultThread = ConversationThread(
                id = threadId,
                name = targetUser.displayName.ifBlank { targetHandle },
                lastMessage = initialMessage,
                timestamp = "Now",
                unreadCount = 0,
                isOnline = true,
                isPinned = false,
                isMuted = false,
                isVerified = true,
                isGroup = false,
                category = ConversationCategory.DIRECT,
                avatarInitials = targetUser.displayName.take(2).uppercase().ifBlank { "VQ" },
                avatarColorStart = 0xFF4F46E5,
                avatarColorEnd = 0xFF2563EB,
                lastSenderName = currentUser.displayName,
                lastSenderId = currentHandle,
                participantIds = listOf(currentHandle, targetHandle).sorted(),
                messageStatus = MessageStatus.SENT,
                snippetType = SnippetType.TEXT
            )

            Log.i(tag, "Successfully added contact @$targetHandle and synced thread $threadId")
            Result.success(resultThread)
        } catch (e: Exception) {
            Log.e(tag, "Failed to add contact: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Dispatches a message to the shared thread and updates the thread's last message.
     */
    suspend fun sendMessage(
        threadId: String,
        senderProfile: UserProfile,
        content: String
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val trimmed = content.trim()
            if (trimmed.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Message cannot be empty."))
            }

            val now = getCurrentIsoTimestamp()
            val senderHandle = senderProfile.customUserId.trim().removePrefix("@").lowercase()
            val messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

            val msgPayload = mapOf(
                "messageId" to messageId,
                "threadId" to threadId,
                "senderId" to senderHandle,
                "senderName" to senderProfile.displayName,
                "senderRole" to if (senderProfile.isTeacher) UserProfile.ROLE_TEACHER else UserProfile.ROLE_STUDENT,
                "content" to trimmed,
                "timestamp" to now
            )

            // Write message subcollection document
            db.collection("threads")
                .document(threadId)
                .collection("messages")
                .document(messageId)
                .set(msgPayload)
                .await()

            // Update thread lastMessage & updatedAt
            val updatePayload = mapOf<String, Any>(
                "lastMessage" to trimmed,
                "lastSenderId" to senderHandle,
                "lastSenderName" to senderProfile.displayName,
                "updatedAt" to now
            )
            db.collection("threads").document(threadId).update(updatePayload).await()

            val created = ChatMessage(
                messageId = messageId,
                threadId = threadId,
                senderId = senderHandle,
                senderName = senderProfile.displayName,
                senderRole = if (senderProfile.isTeacher) UserProfile.ROLE_TEACHER else UserProfile.ROLE_STUDENT,
                content = trimmed,
                timestamp = now
            )
            Result.success(created)
        } catch (e: Exception) {
            Log.e(tag, "Failed to send message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Dispatches a message containing a universal file attachment to the thread.
     */
    suspend fun sendFileMessage(
        threadId: String,
        senderProfile: UserProfile,
        content: String,
        attachmentUri: String,
        attachmentName: String,
        attachmentType: String,
        attachmentSize: String,
        attachmentKey: String? = null
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val now = getCurrentIsoTimestamp()
            val senderHandle = senderProfile.customUserId.trim().removePrefix("@").lowercase()
            val messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

            val displaySnippet = if (content.trim().isNotBlank()) {
                content.trim()
            } else {
                "📎 $attachmentName ($attachmentSize)"
            }

            val msgPayload = mutableMapOf<String, Any?>(
                "messageId" to messageId,
                "threadId" to threadId,
                "senderId" to senderHandle,
                "senderName" to senderProfile.displayName,
                "senderRole" to if (senderProfile.isTeacher) UserProfile.ROLE_TEACHER else UserProfile.ROLE_STUDENT,
                "content" to content.trim(),
                "timestamp" to now,
                "attachmentUri" to attachmentUri,
                "attachmentUrl" to attachmentUri,
                "temporaryDownloadUrl" to attachmentUri,
                "attachmentName" to attachmentName,
                "attachmentType" to attachmentType,
                "attachmentSize" to attachmentSize
            )
            if (attachmentKey != null) {
                msgPayload["attachmentKey"] = attachmentKey
            }

            // Write message document
            db.collection("threads")
                .document(threadId)
                .collection("messages")
                .document(messageId)
                .set(msgPayload)
                .await()

            // Update thread lastMessage & updatedAt
            val updatePayload = mapOf<String, Any>(
                "lastMessage" to displaySnippet,
                "lastSenderId" to senderHandle,
                "lastSenderName" to senderProfile.displayName,
                "updatedAt" to now
            )
            db.collection("threads").document(threadId).update(updatePayload).await()

            val created = ChatMessage(
                messageId = messageId,
                threadId = threadId,
                senderId = senderHandle,
                senderName = senderProfile.displayName,
                senderRole = if (senderProfile.isTeacher) UserProfile.ROLE_TEACHER else UserProfile.ROLE_STUDENT,
                content = content.trim(),
                timestamp = now,
                attachmentUri = attachmentUri,
                attachmentUrl = attachmentUri,
                temporaryDownloadUrl = attachmentUri,
                attachmentKey = attachmentKey,
                attachmentName = attachmentName,
                attachmentType = attachmentType,
                attachmentSize = attachmentSize
            )
            Result.success(created)
        } catch (e: Exception) {
            Log.e(tag, "Failed to send file message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Adds an existing user contact to a group thread.
     */
    suspend fun addMemberToGroup(
        threadId: String,
        newMemberHandle: String,
        addedByProfile: UserProfile
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val handle = newMemberHandle.trim().removePrefix("@").lowercase()
            if (handle.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Member handle cannot be empty."))
            }

            // 1. Add to participantIds array in thread
            db.collection("threads").document(threadId).update(
                mapOf(
                    "participantIds" to FieldValue.arrayUnion(handle),
                    "updatedAt" to getCurrentIsoTimestamp()
                )
            ).await()

            // 2. Add system notification message in thread
            val now = getCurrentIsoTimestamp()
            val msgId = "sys_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            val sysMsgPayload = mapOf(
                "messageId" to msgId,
                "threadId" to threadId,
                "senderId" to "system",
                "senderName" to "DAV InfoExchange",
                "senderRole" to "SYSTEM",
                "content" to "@$handle was added to the group by ${addedByProfile.displayName.ifBlank { "@${addedByProfile.customUserId}" }}.",
                "timestamp" to now
            )
            db.collection("threads").document(threadId).collection("messages").document(msgId).set(sysMsgPayload).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to add member to group: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a message from a thread subcollection.
     */
    suspend fun deleteMessage(
        threadId: String,
        messageId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("threads")
                .document(threadId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Removes a contact from a user's roster and removes reciprocal link.
     */
    suspend fun removeContact(
        userHandle: String,
        contactHandle: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val u = userHandle.trim().removePrefix("@").lowercase()
            val c = contactHandle.trim().removePrefix("@").lowercase()

            db.collection("users")
                .document(u)
                .collection("contacts")
                .document(c)
                .delete()
                .await()

            // Also clean reciprocal contact
            try {
                db.collection("users")
                    .document(c)
                    .collection("contacts")
                    .document(u)
                    .delete()
                    .await()
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to remove contact: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a new collaborative Class Study Circle in Firestore.
     */
    suspend fun createStudyCircle(
        creator: UserProfile,
        circleName: String,
        grade: String,
        section: String,
        subject: String,
        description: String,
        initialMembers: List<String> = emptyList()
    ): Result<ConversationThread> = withContext(Dispatchers.IO) {
        try {
            val creatorHandle = creator.customUserId.trim().removePrefix("@").lowercase()
            val now = getCurrentIsoTimestamp()
            val threadId = "circle_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

            val members = (listOf(creatorHandle) + initialMembers.map { it.trim().removePrefix("@").lowercase() })
                .filter { it.isNotBlank() }
                .distinct()

            val initialMessage = if (description.isNotBlank()) description.trim() else "Study circle initialized for ${if (grade.isNotBlank()) "Class $grade" else "collaborative studies"}"

            val threadPayload = mapOf(
                "threadId" to threadId,
                "groupName" to circleName.trim(),
                "grade" to grade.trim(),
                "section" to section.trim(),
                "subject" to subject.trim(),
                "description" to description.trim(),
                "creatorId" to creatorHandle,
                "adminIds" to listOf(creatorHandle),
                "onlyAdminsCanChat" to false,
                "onlyAdminsCanAddMembers" to false,
                "avatarUri" to null,
                "isGroup" to true,
                "category" to "GROUP",
                "participantIds" to members,
                "lastMessage" to initialMessage,
                "lastSenderId" to creatorHandle,
                "lastSenderName" to creator.displayName.ifBlank { creatorHandle },
                "updatedAt" to now,
                "createdAt" to now
            )

            db.collection("threads").document(threadId).set(threadPayload).await()

            // Handshake message
            val msgId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            val msgPayload = mapOf(
                "messageId" to msgId,
                "threadId" to threadId,
                "senderId" to creatorHandle,
                "senderName" to creator.displayName.ifBlank { creatorHandle },
                "content" to "Welcome to $circleName! Collaborative study exchange established.",
                "timestamp" to now
            )
            db.collection("threads").document(threadId).collection("messages").document(msgId).set(msgPayload).await()

            val created = ConversationThread(
                id = threadId,
                name = circleName.trim(),
                lastMessage = initialMessage,
                timestamp = "Now",
                unreadCount = 0,
                isOnline = true,
                isPinned = false,
                isMuted = false,
                isVerified = true,
                isGroup = true,
                category = ConversationCategory.GROUP,
                avatarInitials = circleName.take(2).uppercase().ifBlank { "SC" },
                avatarColorStart = 0xFF0284C7,
                avatarColorEnd = 0xFF38BDF8,
                lastSenderName = creator.displayName,
                lastSenderId = creatorHandle,
                participantIds = members,
                messageStatus = MessageStatus.SENT,
                snippetType = SnippetType.TEXT,
                creatorId = creatorHandle,
                adminIds = listOf(creatorHandle),
                onlyAdminsCanChat = false,
                onlyAdminsCanAddMembers = false,
                avatarUri = null,
                subject = subject.trim(),
                grade = grade.trim(),
                section = section.trim(),
                description = description.trim()
            )
            Result.success(created)
        } catch (e: Exception) {
            Log.e(tag, "Failed to create study circle: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun observeThread(threadId: String): Flow<ConversationThread?> = callbackFlow {
        val registration = db.collection("threads").document(threadId)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.e(tag, "Error observing thread $threadId: ${error.message}", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val participants = (doc.get("participantIds") as? List<String>) ?: emptyList()
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val lastSenderId = doc.getString("lastSenderId") ?: ""
                    val lastSenderName = doc.getString("lastSenderName") ?: ""
                    val updatedAt = doc.getString("updatedAt") ?: ""
                    val isGroup = doc.getBoolean("isGroup") == true || doc.getString("category") == "GROUP"
                    val groupName = doc.getString("groupName")
                    val rawCategory = doc.getString("category") ?: if (isGroup) "GROUP" else "DIRECT"
                    val category = when (rawCategory) {
                        "GROUP" -> ConversationCategory.GROUP
                        "CHANNEL" -> ConversationCategory.CHANNEL
                        else -> ConversationCategory.DIRECT
                    }
                    val creatorId = doc.getString("creatorId") ?: ""
                    @Suppress("UNCHECKED_CAST")
                    val rawAdminIds = (doc.get("adminIds") as? List<String>) ?: emptyList()
                    val adminIds = if (rawAdminIds.isEmpty() && creatorId.isNotBlank()) listOf(creatorId) else rawAdminIds
                    val onlyAdminsCanChat = doc.getBoolean("onlyAdminsCanChat") == true
                    val onlyAdminsCanAddMembers = doc.getBoolean("onlyAdminsCanAddMembers") == true
                    val avatarUri = doc.getString("avatarUri")
                    val subject = doc.getString("subject") ?: ""
                    val grade = doc.getString("grade") ?: ""
                    val section = doc.getString("section") ?: ""
                    val description = doc.getString("description") ?: ""

                    val displayName = if (isGroup && !groupName.isNullOrBlank()) groupName else "Direct Chat"
                    val initials = displayName.take(2).uppercase().ifBlank { "SC" }

                    trySend(
                        ConversationThread(
                            id = doc.id,
                            name = displayName,
                            lastMessage = lastMessage,
                            timestamp = formatRelativeTime(updatedAt),
                            unreadCount = 0,
                            isOnline = true,
                            isPinned = false,
                            isMuted = false,
                            isVerified = true,
                            isGroup = isGroup,
                            category = category,
                            avatarInitials = initials,
                            avatarColorStart = if (isGroup) 0xFF0284C7 else 0xFF4F46E5,
                            avatarColorEnd = if (isGroup) 0xFF38BDF8 else 0xFF2563EB,
                            lastSenderName = lastSenderName,
                            lastSenderId = lastSenderId,
                            participantIds = participants,
                            messageStatus = MessageStatus.DELIVERED,
                            snippetType = SnippetType.TEXT,
                            creatorId = creatorId,
                            adminIds = adminIds,
                            onlyAdminsCanChat = onlyAdminsCanChat,
                            onlyAdminsCanAddMembers = onlyAdminsCanAddMembers,
                            avatarUri = avatarUri,
                            subject = subject,
                            grade = grade,
                            section = section,
                            description = description
                        )
                    )
                } else {
                    trySend(null)
                }
            }
        awaitClose { registration.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun updateGroupSettings(
        threadId: String,
        name: String,
        description: String,
        subject: String,
        grade: String,
        section: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf(
                "groupName" to name.trim(),
                "description" to description.trim(),
                "subject" to subject.trim(),
                "grade" to grade.trim(),
                "section" to section.trim(),
                "updatedAt" to getCurrentIsoTimestamp()
            )
            db.collection("threads").document(threadId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update group settings: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateGroupAvatar(
        threadId: String,
        avatarUri: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf(
                "avatarUri" to avatarUri,
                "updatedAt" to getCurrentIsoTimestamp()
            )
            db.collection("threads").document(threadId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update group avatar: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateGroupPermissions(
        threadId: String,
        onlyAdminsCanChat: Boolean,
        onlyAdminsCanAddMembers: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf(
                "onlyAdminsCanChat" to onlyAdminsCanChat,
                "onlyAdminsCanAddMembers" to onlyAdminsCanAddMembers,
                "updatedAt" to getCurrentIsoTimestamp()
            )
            db.collection("threads").document(threadId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update group permissions: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun setGroupMemberAdmin(
        threadId: String,
        memberHandle: String,
        makeAdmin: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanHandle = memberHandle.trim().removePrefix("@").lowercase()
            val updateAction = if (makeAdmin) {
                FieldValue.arrayUnion(cleanHandle)
            } else {
                FieldValue.arrayRemove(cleanHandle)
            }
            db.collection("threads").document(threadId).update(
                mapOf(
                    "adminIds" to updateAction,
                    "updatedAt" to getCurrentIsoTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to change member admin status: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun removeMemberFromGroup(
        threadId: String,
        memberHandle: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanHandle = memberHandle.trim().removePrefix("@").lowercase()
            db.collection("threads").document(threadId).update(
                mapOf(
                    "participantIds" to FieldValue.arrayRemove(cleanHandle),
                    "adminIds" to FieldValue.arrayRemove(cleanHandle),
                    "updatedAt" to getCurrentIsoTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to remove member from group: ${e.message}", e)
            Result.failure(e)
        }
    }
}
