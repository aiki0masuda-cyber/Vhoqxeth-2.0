package com.example.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MeshChatRepository
import com.example.model.ChatMessage
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

data class SelectedFileAttachment(
    val uri: Uri,
    val name: String,
    val size: String,
    val type: String
)

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

    // Live messages stream from Firestore
    val messagesFlow = remember(thread.id) {
        chatRepository.observeMessages(thread.id)
    }
    val messages by messagesFlow.collectAsState(initial = emptyList())

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val currentHandle = currentUser.customUserId.trim().removePrefix("@").lowercase()

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
                    lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".txt") -> "DOC"
                    lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".csv") -> "SHEET"
                    lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".tar") -> "ARCHIVE"
                    else -> "FILE"
                }

                pendingAttachment = SelectedFileAttachment(
                    uri = uri,
                    name = displayName,
                    size = formattedSize,
                    type = inferredType
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
                    .background(if (thread.isGroup) DavAccentSky else DavBrandRoyalBlue),
                contentAlignment = Alignment.Center
            ) {
                if (thread.isGroup) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Group",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = thread.avatarInitials.ifBlank { "DAV" },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = thread.name,
                        color = DavTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (thread.isFaculty) {
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
                    if (thread.isGroup) {
                        Text(
                            text = "${thread.participantIds.size} members • Collaborative Circle",
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

            // If it is a group, show "Add Members" action button
            if (thread.isGroup) {
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

            items(messages, key = { it.messageId }) { msg ->
                val isMe = msg.senderId.lowercase() == currentHandle
                MessageBubble(
                    message = msg,
                    isMe = isMe,
                    context = context
                )
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

            Spacer(modifier = Modifier.width(8.dp))

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
                        isSending = true
                        val text = inputMessage.trim()
                        val att = pendingAttachment
                        inputMessage = ""
                        pendingAttachment = null

                        coroutineScope.launch {
                            if (att != null) {
                                chatRepository.sendFileMessage(
                                    threadId = thread.id,
                                    senderProfile = currentUser,
                                    content = text,
                                    attachmentUri = att.uri.toString(),
                                    attachmentName = att.name,
                                    attachmentType = att.type,
                                    attachmentSize = att.size
                                )
                            } else {
                                chatRepository.sendMessage(thread.id, currentUser, text)
                            }
                            isSending = false
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

    // Modal: Add members to this group
    if (showAddMemberDialog) {
        AddMemberDialog(
            thread = thread,
            currentUser = currentUser,
            chatRepository = chatRepository,
            onDismiss = { showAddMemberDialog = false },
            onMemberAdded = {
                // Member added successfully
            }
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
                                if (!message.attachmentUri.isNullOrBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(message.attachmentUri), "*/*")
                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Open $attName"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
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
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open file",
                                tint = if (isMe) Color.White else DavBrandRoyalBlue,
                                modifier = Modifier.size(18.dp)
                            )
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
        "DOC" -> Icons.Default.Description
        "SHEET" -> Icons.Default.TableChart
        "ARCHIVE" -> Icons.Default.FolderZip
        else -> Icons.Default.Description
    }
}
