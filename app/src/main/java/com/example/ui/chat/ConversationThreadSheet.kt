package com.example.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.model.UploadState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.AiImageGeneratorEngine
import com.example.ai.SiliconFlowImageEngine
import com.example.ai.SiliconFlowModel
import com.example.data.CatboxUploadManager
import com.example.data.MeshChatRepository
import com.example.data.SessionManager
import com.example.model.ChatMessage
import com.example.util.FileAttachmentActionManager
import com.example.model.ConversationThread
import com.example.model.UserProfile
import com.example.ui.theme.DavAccentSky
import com.example.ui.theme.DavBorder
import com.example.ui.theme.DavBrandRoyalBlue
import com.example.ui.theme.DavCanvasLight
import com.example.ui.theme.DavSurfaceElevated
import com.example.ui.theme.DavTextMuted
import com.example.ui.theme.DavTextPrimary
import com.example.ui.theme.DavTextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SelectedFileAttachment(
    val uri: Uri,
    val name: String,
    val size: String,
    val type: String,
    val sizeBytes: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationThreadSheet(
    thread: ConversationThread,
    currentUser: UserProfile,
    chatRepository: MeshChatRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputMessage by remember { mutableStateOf("") }
    var pendingAttachment by remember { mutableStateOf<SelectedFileAttachment?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showAiImageDialog by remember { mutableStateOf(false) }
    var showGroupInfoSheet by remember { mutableStateOf(false) }
    var showCloudflareWorkerSettings by remember { mutableStateOf(false) }

    // Live thread stream from Firestore
    val liveThreadState by chatRepository.observeThread(thread.id).collectAsState(initial = thread)
    val currentThread = liveThreadState ?: thread

    val currentHandle = currentUser.customUserId.trim().removePrefix("@").lowercase()
    val canUserChat = currentThread.canUserChat(currentHandle)
    val canUserAddMembers = currentThread.canUserAddMembers(currentHandle)

    // Live messages stream from Firestore
    val messagesFlow = remember(thread.id) {
        chatRepository.observeMessages(thread.id)
    }
    val messages by messagesFlow.collectAsState(initial = emptyList())

    val sessionManager = remember { SessionManager(context) }
    val catboxUploadManager = remember { CatboxUploadManager() }
    val pendingUploads = remember { mutableStateListOf<ChatMessage>() }
    val uploadStates = remember { mutableStateMapOf<String, UploadState>() }

    // Combined live stream: Firestore confirmed messages + in-memory optimistic upload placeholders
    val combinedMessages = remember(messages, pendingUploads.toList()) {
        messages + pendingUploads
    }

    // Auto-scroll to bottom on new messages or new upload placeholder
    LaunchedEffect(combinedMessages.size) {
        if (combinedMessages.isNotEmpty()) {
            listState.animateScrollToItem(combinedMessages.size - 1)
        }
    }

    // Universal System Document Picker Launcher (Supports all file types: PDF, docs, images, zips, spreadsheets)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Persist read access
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                var displayName = "Attachment"
                var sizeBytes = 0L

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: "Attachment"
                        if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                    }
                }

                val formattedSize = when {
                    sizeBytes > 1024 * 1024 -> String.format(Locale.US, "%.1f MB", sizeBytes / (1024f * 1024f))
                    sizeBytes > 1024 -> "${sizeBytes / 1024} KB"
                    sizeBytes > 0 -> "$sizeBytes B"
                    else -> "Document"
                }

                val lower = displayName.lowercase()
                val inferredType = when {
                    lower.endsWith(".pdf") -> "PDF"
                    lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") -> "IMAGE"
                    lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".avi") -> "VIDEO"
                    lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".txt") -> "DOC"
                    lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".csv") -> "SHEET"
                    lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".tar") -> "ARCHIVE"
                    else -> "FILE"
                }

                pendingAttachment = SelectedFileAttachment(
                    uri = uri,
                    name = displayName,
                    size = formattedSize,
                    type = inferredType,
                    sizeBytes = sizeBytes
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .background(DavCanvasLight)
            .navigationBarsPadding()
            .imePadding()
            .testTag("conversation_thread_sheet")
    ) {
        // Sheet Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (currentThread.isGroup) DavAccentSky else DavBrandRoyalBlue)
                    .clickable(enabled = currentThread.isGroup) {
                        showGroupInfoSheet = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!currentThread.avatarUri.isNullOrBlank()) {
                    AsyncImage(
                        model = currentThread.avatarUri,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                } else if (currentThread.isGroup) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Group",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = currentThread.avatarInitials.ifBlank { "DAV" },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = currentThread.isGroup) {
                        showGroupInfoSheet = true
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentThread.name,
                        color = DavTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (currentThread.isFaculty) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DavAccentSky.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "FACULTY",
                                color = DavAccentSky,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentThread.isGroup) {
                        Text(
                            text = "${currentThread.participantIds.size} members" +
                                    (if (currentThread.subject.isNotBlank()) " • ${currentThread.subject}" else "") +
                                    (if (currentThread.onlyAdminsCanChat) " • Admins only" else ""),
                            color = DavAccentSky,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "DAV Direct Exchange • Active",
                            color = DavTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Group Info / Settings button
            if (currentThread.isGroup) {
                IconButton(
                    onClick = { showGroupInfoSheet = true },
                    modifier = Modifier.testTag("group_info_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Group Info & Settings",
                        tint = DavBrandRoyalBlue
                    )
                }
            }

            // If it is a group and user has permission to add members, show "Add Members" action button
            if (currentThread.isGroup && canUserAddMembers) {
                IconButton(
                    onClick = { showAddMemberDialog = true },
                    modifier = Modifier.testTag("add_group_member_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Members to Group",
                        tint = DavBrandRoyalBlue
                    )
                }
            }

            // Cloudflare Worker Gatekeeper Settings
            IconButton(
                onClick = { showCloudflareWorkerSettings = true },
                modifier = Modifier.testTag("open_cloudflare_worker_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = "Cloudflare Worker Gatekeeper Settings",
                    tint = DavBrandRoyalBlue
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_thread_sheet_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = DavTextSecondary
                )
            }
        }

        HorizontalDivider(color = DavBorder, thickness = 0.5.dp)

        // Security encryption notice
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DavSurfaceElevated)
                .padding(vertical = 6.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = DavBrandRoyalBlue,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DAV InfoExchange E2EE Synchronized • Session Active",
                    color = DavTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        HorizontalDivider(color = DavBorder, thickness = 0.5.dp)

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (thread.isGroup) "Welcome to ${thread.name}" else "Start a scholastic conversation",
                                color = DavTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Send messages, homework files, PDFs, and notes securely",
                                color = DavTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            items(combinedMessages, key = { it.messageId }) { msg ->
                val isMe = msg.senderId.lowercase() == currentHandle
                if (msg.isUploadingPlaceholder) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        UploadingPlaceholderBubble(
                            message = msg,
                            uploadState = uploadStates[msg.messageId],
                            onCancel = {
                                pendingUploads.removeAll { it.messageId == msg.messageId }
                                uploadStates.remove(msg.messageId)
                            },
                            onRetry = {
                                coroutineScope.launch {
                                    val attUri = Uri.parse(msg.attachmentUri ?: "")
                                    val uploadRes = catboxUploadManager.uploadFile(
                                        contentResolver = context.contentResolver,
                                        fileUri = attUri,
                                        filename = msg.attachmentName ?: "attachment",
                                        contentType = when (msg.attachmentType) {
                                            "IMAGE" -> "image/jpeg"
                                            "VIDEO" -> "video/mp4"
                                            "PDF" -> "application/pdf"
                                            else -> "application/octet-stream"
                                        },
                                        onProgress = { p ->
                                            uploadStates[msg.messageId] = UploadState.Uploading(p)
                                            val idx = pendingUploads.indexOfFirst { it.messageId == msg.messageId }
                                            if (idx != -1) {
                                                pendingUploads[idx] = pendingUploads[idx].copy(uploadProgress = p)
                                            }
                                        }
                                    )
                                    if (uploadRes.isLocalFallback && uploadRes.errorMessage != null) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, uploadRes.errorMessage, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    chatRepository.sendFileMessage(
                                        threadId = currentThread.id,
                                        senderProfile = currentUser,
                                        content = msg.content,
                                        attachmentUri = uploadRes.url,
                                        attachmentName = msg.attachmentName ?: "File",
                                        attachmentType = msg.attachmentType ?: "FILE",
                                        attachmentSize = msg.attachmentSize ?: "",
                                        attachmentKey = null
                                    )
                                    pendingUploads.removeAll { it.messageId == msg.messageId }
                                    uploadStates.remove(msg.messageId)
                                }
                            }
                        )
                    }
                } else {
                    MessageBubble(
                        message = msg,
                        isMe = isMe,
                        context = context
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        HorizontalDivider(color = DavBorder, thickness = 0.5.dp)

        // Pending Attachment Banner (when user picked a file)
        if (pendingAttachment != null) {
            val att = pendingAttachment!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0F2FE))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getAttachmentIcon(att.type),
                    contentDescription = null,
                    tint = DavBrandRoyalBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = att.name,
                        color = DavTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${att.type} • ${att.size}",
                        color = DavBrandRoyalBlue,
                        fontSize = 11.sp
                    )
                }
                IconButton(
                    onClick = { pendingAttachment = null },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attachment",
                        tint = DavTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            HorizontalDivider(color = DavBorder, thickness = 0.5.dp)
        }

        if (!canUserChat) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DavSurfaceElevated)
                        .border(1.dp, DavBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = DavBrandRoyalBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Only group admins can send messages to this group",
                        color = DavTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // Bottom Message & Attachment Composer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment Button (Universal Document Picker)
                IconButton(
                    onClick = {
                        documentPickerLauncher.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DavSurfaceElevated)
                        .testTag("attach_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach File / Document",
                        tint = DavBrandRoyalBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // AI Image Generation Sparkle Button
                IconButton(
                    onClick = { showAiImageDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DavAccentSky.copy(alpha = 0.15f))
                        .testTag("ai_image_sparkle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Generate Study Diagram with AI",
                        tint = DavBrandRoyalBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = {
                        Text(
                            text = if (pendingAttachment != null) "Add a caption (optional)..." else "Type a message...",
                            color = DavTextMuted,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DavSurfaceElevated,
                        unfocusedContainerColor = DavSurfaceElevated,
                        focusedBorderColor = DavBrandRoyalBlue,
                        unfocusedBorderColor = DavBorder,
                        focusedTextColor = DavTextPrimary,
                        unfocusedTextColor = DavTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                val canSend = (inputMessage.trim().isNotBlank() || pendingAttachment != null) && !isSending

                IconButton(
                    onClick = {
                        if (canSend) {
                            val text = inputMessage.trim()
                            val att = pendingAttachment
                            inputMessage = ""
                            pendingAttachment = null

                            if (att != null) {
                                val tempId = "upload_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                                val placeholder = ChatMessage(
                                    messageId = tempId,
                                    threadId = currentThread.id,
                                    senderId = currentHandle,
                                    senderName = currentUser.displayName,
                                    content = text,
                                    timestamp = "Uploading...",
                                    attachmentUri = att.uri.toString(),
                                    attachmentName = att.name,
                                    attachmentType = att.type,
                                    attachmentSize = att.size,
                                    isUploadingPlaceholder = true,
                                    uploadProgress = 0f
                                )
                                pendingUploads.add(placeholder)

                                coroutineScope.launch {
                                    val uploadRes = catboxUploadManager.uploadFile(
                                        contentResolver = context.contentResolver,
                                        fileUri = att.uri,
                                        filename = att.name,
                                        contentType = when (att.type) {
                                            "IMAGE" -> "image/jpeg"
                                            "VIDEO" -> "video/mp4"
                                            "PDF" -> "application/pdf"
                                            else -> "application/octet-stream"
                                        },
                                        onProgress = { progress ->
                                            uploadStates[tempId] = UploadState.Uploading(progress)
                                            val idx = pendingUploads.indexOfFirst { it.messageId == tempId }
                                            if (idx != -1) {
                                                pendingUploads[idx] = pendingUploads[idx].copy(uploadProgress = progress)
                                            }
                                        }
                                    )

                                    if (uploadRes.isLocalFallback && uploadRes.errorMessage != null) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, uploadRes.errorMessage, Toast.LENGTH_LONG).show()
                                        }
                                    }

                                    chatRepository.sendFileMessage(
                                        threadId = currentThread.id,
                                        senderProfile = currentUser,
                                        content = text,
                                        attachmentUri = uploadRes.url,
                                        attachmentName = att.name,
                                        attachmentType = att.type,
                                        attachmentSize = att.size,
                                        attachmentKey = null
                                    )

                                    pendingUploads.removeAll { it.messageId == tempId }
                                    uploadStates.remove(tempId)
                                }
                            } else {
                                val imageCommandPrompt = SiliconFlowImageEngine.extractImageCommandPrompt(text)
                                if (imageCommandPrompt != null) {
                                    // User typed /imagine, /draw, /image, or natural language generation request
                                    isSending = true
                                    coroutineScope.launch {
                                        try {
                                            Toast.makeText(
                                                context,
                                                "🎨 Synthesizing illustration: \"$imageCommandPrompt\" with SiliconFlow FLUX.1 Schnell...",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            val result = AiImageGeneratorEngine.generateImage(
                                                prompt = imageCommandPrompt,
                                                model = SiliconFlowModel.FLUX_1_SCHNELL
                                            )

                                            if (result.isSuccess) {
                                                val bitmap = result.getOrNull()!!
                                                val cachedUri = AiImageGeneratorEngine.saveBitmapToLocalCache(
                                                    context = context,
                                                    bitmap = bitmap,
                                                    prefix = "chat_ai_${currentThread.id}"
                                                )
                                                val attName = "illustration_${System.currentTimeMillis()}.png"

                                                val uploadRes = catboxUploadManager.uploadFile(
                                                    contentResolver = context.contentResolver,
                                                    fileUri = cachedUri,
                                                    filename = attName,
                                                    contentType = "image/png"
                                                )

                                                if (uploadRes.isLocalFallback && uploadRes.errorMessage != null) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, uploadRes.errorMessage, Toast.LENGTH_LONG).show()
                                                    }
                                                }

                                                chatRepository.sendFileMessage(
                                                    threadId = currentThread.id,
                                                    senderProfile = currentUser,
                                                    content = "🎨 Prompt: \"$imageCommandPrompt\"",
                                                    attachmentUri = uploadRes.url,
                                                    attachmentName = attName,
                                                    attachmentType = "IMAGE",
                                                    attachmentSize = "AI Generated PNG",
                                                    attachmentKey = null
                                                )
                                            } else {
                                                val ex = result.exceptionOrNull()
                                                val errMsg = ex?.localizedMessage ?: "Failed to generate image."
                                                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                                                chatRepository.sendMessage(
                                                    currentThread.id,
                                                    currentUser,
                                                    "⚠️ [Image Generation]: $errMsg"
                                                )
                                            }
                                        } finally {
                                            isSending = false
                                        }
                                    }
                                } else {
                                    isSending = true
                                    coroutineScope.launch {
                                        chatRepository.sendMessage(currentThread.id, currentUser, text)
                                        isSending = false
                                    }
                                }
                            }
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) DavBrandRoyalBlue else Color(0xFFE2E8F0)
                        )
                        .testTag("send_message_button")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (canSend) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal: Add members to this group
    if (showAddMemberDialog) {
        AddMemberDialog(
            thread = currentThread,
            currentUser = currentUser,
            chatRepository = chatRepository,
            onDismiss = { showAddMemberDialog = false },
            onMemberAdded = {
                // Member added successfully
            }
        )
    }

    // Modal: AI Study Illustration Generator
    if (showAiImageDialog) {
        val suggestedPrompt = if (currentThread.isGroup && currentThread.subject.isNotBlank()) {
            "Educational concept illustration explaining ${currentThread.subject}"
        } else ""

        AiGenerateImageDialog(
            initialPrompt = suggestedPrompt,
            title = "Generate Study Diagram with AI",
            confirmButtonLabel = "Send into Chat",
            onDismiss = { showAiImageDialog = false },
            onImageAccepted = { bitmap, prompt ->
                val cachedUri = AiImageGeneratorEngine.saveBitmapToLocalCache(
                    context = context,
                    bitmap = bitmap,
                    prefix = "chat_ai_${currentThread.id}"
                )
                val tempId = "ai_upload_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                val attName = "AI_${prompt.take(20).replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")}.png"
                val placeholder = ChatMessage(
                    messageId = tempId,
                    threadId = currentThread.id,
                    senderId = currentHandle,
                    senderName = currentUser.displayName,
                    content = prompt,
                    timestamp = "Uploading...",
                    attachmentUri = cachedUri.toString(),
                    attachmentName = attName,
                    attachmentType = "IMAGE",
                    attachmentSize = "AI Generated PNG",
                    isUploadingPlaceholder = true,
                    uploadProgress = 0f
                )
                pendingUploads.add(placeholder)

                coroutineScope.launch {
                    val uploadRes = catboxUploadManager.uploadFile(
                        contentResolver = context.contentResolver,
                        fileUri = cachedUri,
                        filename = attName,
                        contentType = "image/png",
                        onProgress = { progress ->
                            uploadStates[tempId] = UploadState.Uploading(progress)
                            val idx = pendingUploads.indexOfFirst { it.messageId == tempId }
                            if (idx != -1) {
                                pendingUploads[idx] = pendingUploads[idx].copy(uploadProgress = progress)
                            }
                        }
                    )

                    if (uploadRes.isLocalFallback && uploadRes.errorMessage != null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, uploadRes.errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }

                    chatRepository.sendFileMessage(
                        threadId = currentThread.id,
                        senderProfile = currentUser,
                        content = prompt,
                        attachmentUri = uploadRes.url,
                        attachmentName = attName,
                        attachmentType = "IMAGE",
                        attachmentSize = "AI Generated PNG",
                        attachmentKey = null
                    )

                    pendingUploads.removeAll { it.messageId == tempId }
                    uploadStates.remove(tempId)
                }
            }
        )
    }

    // Modal Bottom Sheet: Group Info & Settings
    if (showGroupInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGroupInfoSheet = false },
            containerColor = Color.White
        ) {
            GroupInfoSettingsSheet(
                initialThread = currentThread,
                currentUser = currentUser,
                chatRepository = chatRepository,
                onAddMembersRequested = {
                    showGroupInfoSheet = false
                    showAddMemberDialog = true
                },
                onDismiss = { showGroupInfoSheet = false }
            )
        }
    }

    // Dialog: Cloudflare Worker Gatekeeper Settings
    if (showCloudflareWorkerSettings) {
        CloudflareWorkerSettingsDialog(
            currentUrl = sessionManager.getCloudflareWorkerUrl(),
            onSaveUrl = { newUrl ->
                sessionManager.setCloudflareWorkerUrl(newUrl)
                Toast.makeText(context, "Storage gatekeeper URL updated", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCloudflareWorkerSettings = false }
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    context: Context
) {
    val bubbleColor = if (isMe) DavBrandRoyalBlue else Color.White
    val textColor = if (isMe) Color.White else DavTextPrimary
    val alignment = if (isMe) Alignment.End else Alignment.Start

    val coroutineScope = rememberCoroutineScope()

    // Direct CDN URL from Catbox or local fallback (with ISP bypass mirror)
    val rawMediaUrl = message.resolvedAttachmentUrl?.ifBlank { null } ?: message.attachmentUri ?: ""
    val activeMediaUrl: String = CatboxUploadManager.applyBypassMirror(rawMediaUrl)

    val timeString = try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val date = format.parse(message.timestamp)
        if (date != null) {
            SimpleDateFormat("h:mm a", Locale.US).format(date)
        } else {
            ""
        }
    } catch (_: Exception) {
        ""
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isMe && message.senderName.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            ) {
                Text(
                    text = message.senderName,
                    color = DavBrandRoyalBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (message.isFaculty) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "[FACULTY]",
                        color = DavAccentSky,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .border(
                    width = if (isMe) 0.dp else 1.dp,
                    color = if (isMe) Color.Transparent else DavBorder,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // If message contains a file attachment
                if (message.hasAttachment) {
                    val attName = message.attachmentName ?: "Attached File"
                    val attType = message.attachmentType ?: "FILE"
                    val attSize = message.attachmentSize ?: ""
                    val effectiveMediaUri = activeMediaUrl.ifBlank { message.resolvedAttachmentUrl ?: "" }

                    if (attType == "IMAGE" && effectiveMediaUri.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    if (isMe) Color.White.copy(alpha = 0.3f) else DavBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    FileAttachmentActionManager.openAttachment(
                                        context = context,
                                        urlOrUri = effectiveMediaUri,
                                        fileName = attName,
                                        fallbackType = "IMAGE"
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = effectiveMediaUri,
                                contentDescription = attName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Action Type 2: Direct download button for images
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        FileAttachmentActionManager.downloadToDownloadsFolder(
                                            context = context,
                                            urlOrUri = effectiveMediaUri,
                                            suggestedFileName = attName,
                                            fallbackType = "IMAGE"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(34.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    .testTag("download_image_attachment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Save image to Downloads",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMe) Color.White.copy(alpha = 0.15f) else DavSurfaceElevated)
                            .border(
                                1.dp,
                                if (isMe) Color.White.copy(alpha = 0.3f) else DavBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                if (effectiveMediaUri.isNotBlank()) {
                                    FileAttachmentActionManager.openAttachment(
                                        context = context,
                                        urlOrUri = effectiveMediaUri,
                                        fileName = attName,
                                        fallbackType = attType
                                    )
                                }
                            }
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isMe) Color.White else DavBrandRoyalBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getAttachmentIcon(attType),
                                    contentDescription = null,
                                    tint = if (isMe) DavBrandRoyalBlue else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = attName,
                                    color = if (isMe) Color.White else DavTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$attType • $attSize",
                                    color = if (isMe) Color.White.copy(alpha = 0.8f) else DavTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            // Action Type 2: Physical System Folder Download
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        FileAttachmentActionManager.downloadToDownloadsFolder(
                                            context = context,
                                            urlOrUri = effectiveMediaUri,
                                            suggestedFileName = attName,
                                            fallbackType = attType
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("download_file_attachment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Save file to Downloads",
                                    tint = if (isMe) Color.White else DavBrandRoyalBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Action Type 1: Direct File Viewing / Web Browser Opening
                            IconButton(
                                onClick = {
                                    FileAttachmentActionManager.openAttachment(
                                        context = context,
                                        urlOrUri = effectiveMediaUri,
                                        fileName = attName,
                                        fallbackType = attType
                                    )
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("open_file_attachment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open file",
                                    tint = if (isMe) Color.White else DavBrandRoyalBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                if (timeString.isNotBlank()) {
                    Text(
                        text = timeString,
                        color = if (isMe) Color.White.copy(alpha = 0.75f) else DavTextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

private fun getAttachmentIcon(type: String): ImageVector {
    return when (type.uppercase()) {
        "PDF" -> Icons.Default.PictureAsPdf
        "IMAGE" -> Icons.Default.Image
        "VIDEO" -> Icons.Default.Videocam
        "DOC" -> Icons.Default.Description
        "SHEET" -> Icons.Default.TableChart
        "ARCHIVE" -> Icons.Default.FolderZip
        else -> Icons.Default.Description
    }
}
