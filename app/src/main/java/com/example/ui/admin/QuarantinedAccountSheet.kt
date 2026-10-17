package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminRepository
import com.example.data.MeshChatRepository
import com.example.model.ChatMessage
import com.example.model.ConversationThread
import com.example.model.UserContact
import com.example.model.UserProfile
import com.example.ui.theme.VhoqxethBorder
import com.example.ui.theme.VhoqxethCyanAccent
import com.example.ui.theme.VhoqxethCyanBright
import com.example.ui.theme.VhoqxethDarkBg
import com.example.ui.theme.VhoqxethOnlineGreen
import com.example.ui.theme.VhoqxethPrimaryViolet
import com.example.ui.theme.VhoqxethSurface
import com.example.ui.theme.VhoqxethSurfaceElevated
import com.example.ui.theme.VhoqxethTextMuted
import com.example.ui.theme.VhoqxethTextPrimary
import com.example.ui.theme.VhoqxethTextSecondary
import com.example.data.PasswordSecurityUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuarantinedAccountSheet(
    targetUser: UserProfile,
    onDismiss: () -> Unit,
    onUserModified: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val chatRepository = remember { MeshChatRepository(context) }
    val adminRepository = remember { AdminRepository(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val cleanHandle = targetUser.customUserId.trim().removePrefix("@").lowercase()

    // Real-time streams for the quarantined account
    val userThreads by chatRepository.observeThreads(cleanHandle).collectAsState(initial = emptyList())
    val userContacts by chatRepository.observeContacts(cleanHandle).collectAsState(initial = emptyList())

    // Tabs: 0: Messages & Threads, 1: Contacts Roster, 2: Identity & Security, 3: Enclave Audit
    var selectedTab by remember { mutableIntStateOf(0) }

    // Thread inspector state
    var activeThread by remember { mutableStateOf<ConversationThread?>(null) }

    // Enclave audit log
    val auditLog = remember {
        mutableStateListOf(
            "QUARANTINE ESTABLISHED: Root enclave connected to @$cleanHandle (${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())})"
        )
    }

    fun logAction(action: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        auditLog.add(0, "[$time] $action")
    }

    // Modal dialogs
    var showAddContactModal by remember { mutableStateOf(false) }
    var showPasswordResetModal by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AdminSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AdminAmber.copy(alpha = 0.6f))
            )
        },
        modifier = Modifier.testTag("quarantined_account_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            // Quarantine Banner Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AdminCrimson.copy(alpha = 0.15f))
                    .border(1.dp, AdminCrimson.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AdminCrimson.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Quarantine Enclave",
                        tint = AdminCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "QUARANTINE ENCLAVE",
                            color = AdminCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AdminAmber)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "ROOT LEVEL",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Text(
                        text = "${targetUser.displayName} (@${targetUser.customUserId})",
                        color = VhoqxethTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("close_quarantine_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Quarantine",
                        tint = VhoqxethTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = AdminCardBg,
                contentColor = AdminAmber,
                divider = { HorizontalDivider(thickness = 0.5.dp, color = VhoqxethBorder) },
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTab),
                        color = AdminAmber
                    )
                }
            ) {
                val tabTitles = listOf("Messages", "Contacts", "Settings", "Audit")
                val tabIcons = listOf(
                    Icons.AutoMirrored.Filled.Chat,
                    Icons.Default.People,
                    Icons.Default.Settings,
                    Icons.Default.History
                )

                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            activeThread = null
                        },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) AdminAmber else VhoqxethTextMuted
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = tabIcons[index],
                                contentDescription = title,
                                tint = if (selectedTab == index) AdminAmber else VhoqxethTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("quarantine_tab_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> {
                        if (activeThread != null) {
                            QuarantineThreadInspector(
                                targetUser = targetUser,
                                thread = activeThread!!,
                                chatRepository = chatRepository,
                                onBack = { activeThread = null },
                                onLog = ::logAction
                            )
                        } else {
                            QuarantineThreadsList(
                                targetUser = targetUser,
                                threads = userThreads,
                                onOpenThread = { thread ->
                                    activeThread = thread
                                    logAction("Opened conversation thread: ${thread.id}")
                                }
                            )
                        }
                    }
                    1 -> {
                        QuarantineContactsRoster(
                            targetUser = targetUser,
                            contacts = userContacts,
                            chatRepository = chatRepository,
                            onOpenAddContact = { showAddContactModal = true },
                            onLog = ::logAction
                        )
                    }
                    2 -> {
                        QuarantineIdentityView(
                            targetUser = targetUser,
                            adminRepository = adminRepository,
                            onPasswordResetRequest = { showPasswordResetModal = true },
                            onUserUpdated = {
                                onUserModified()
                                logAction("Updated profile attributes for @$cleanHandle")
                            },
                            onLog = ::logAction
                        )
                    }
                    3 -> {
                        QuarantineAuditLogView(logs = auditLog)
                    }
                }
            }
        }
    }

    // Modal Dialog: Add Peer to Quarantined Account Roster
    if (showAddContactModal) {
        QuarantineAddContactDialog(
            targetUser = targetUser,
            chatRepository = chatRepository,
            onDismiss = { showAddContactModal = false },
            onContactAdded = { peerHandle ->
                showAddContactModal = false
                logAction("Injected reciprocal contact @$peerHandle into @$cleanHandle's roster")
            }
        )
    }

    // Modal Dialog: Password Reset / Override
    if (showPasswordResetModal) {
        QuarantinePasswordOverrideDialog(
            targetUser = targetUser,
            adminRepository = adminRepository,
            onDismiss = { showPasswordResetModal = false },
            onPasswordOverridden = {
                showPasswordResetModal = false
                onUserModified()
                logAction("Admin overrode and updated cryptographic password hash for @$cleanHandle")
            }
        )
    }
}

@Composable
private fun QuarantineThreadsList(
    targetUser: UserProfile,
    threads: List<ConversationThread>,
    onOpenThread: (ConversationThread) -> Unit
) {
    if (threads.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = VhoqxethTextMuted,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No active conversations found",
                    color = VhoqxethTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Account @${targetUser.customUserId} has no open chat threads.",
                    color = VhoqxethTextMuted,
                    fontSize = 13.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(threads, key = { it.id }) { thread ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenThread(thread) }
                        .testTag("quarantine_thread_${thread.id}"),
                    colors = CardDefaults.cardColors(containerColor = AdminCardBg),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VhoqxethBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VhoqxethPrimaryViolet),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = thread.avatarInitials.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = thread.name,
                                    color = VhoqxethTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = thread.timestamp,
                                    color = VhoqxethTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = thread.lastMessage.ifBlank { "Session ready" },
                                color = VhoqxethCyanBright,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AdminAmber.copy(alpha = 0.2f))
                                .border(1.dp, AdminAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "INTERCEPT",
                                color = AdminAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuarantineThreadInspector(
    targetUser: UserProfile,
    thread: ConversationThread,
    chatRepository: MeshChatRepository,
    onBack: () -> Unit,
    onLog: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val messages by chatRepository.observeMessages(thread.id).collectAsState(initial = emptyList())
    var composeText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    var deletingMessage by remember { mutableStateOf<ChatMessage?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Thread header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AdminCardBg)
                .border(1.dp, VhoqxethBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(32.dp).testTag("quarantine_back_to_threads")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AdminAmber
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "INTERCEPTING: ${thread.name}",
                    color = AdminAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Thread ID: ${thread.id}",
                    color = VhoqxethTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Messages list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(messages, key = { it.messageId }) { msg ->
                val isFromQuarantinedUser = msg.senderId == targetUser.customUserId.trim().removePrefix("@").lowercase()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quarantine_msg_${msg.messageId}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFromQuarantinedUser) AdminSurface else AdminCardBg
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (isFromQuarantinedUser) AdminAmber.copy(alpha = 0.5f) else VhoqxethBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isFromQuarantinedUser) "@${targetUser.customUserId} (Target)" else msg.senderName.ifBlank { msg.senderId },
                                    color = if (isFromQuarantinedUser) AdminAmber else VhoqxethCyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg.timestamp.takeLast(12),
                                    color = VhoqxethTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = msg.content,
                                color = VhoqxethTextPrimary,
                                fontSize = 13.sp
                            )
                        }

                        IconButton(
                            onClick = { deletingMessage = msg },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("delete_msg_${msg.messageId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Message",
                                tint = AdminCrimson,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Send message input field as the quarantined user
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = composeText,
                onValueChange = { composeText = it },
                placeholder = { Text("Send message as @${targetUser.customUserId}...", color = VhoqxethTextMuted, fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("quarantine_send_input"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AdminCardBg,
                    unfocusedContainerColor = AdminCardBg,
                    focusedBorderColor = AdminAmber,
                    unfocusedBorderColor = VhoqxethBorder,
                    focusedTextColor = VhoqxethTextPrimary,
                    unfocusedTextColor = VhoqxethTextPrimary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (composeText.isNotBlank()) {
                        isSending = true
                        val textToSend = composeText
                        coroutineScope.launch {
                            val res = chatRepository.sendMessage(thread.id, targetUser, textToSend)
                            if (res.isSuccess) {
                                composeText = ""
                                onLog("Dispatched message to ${thread.id} under persona @${targetUser.customUserId}")
                            }
                            isSending = false
                        }
                    }
                },
                enabled = composeText.isNotBlank() && !isSending,
                colors = ButtonDefaults.buttonColors(containerColor = AdminAmber),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("quarantine_send_button")
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black)
                }
            }
        }
    }

    // Confirm message deletion dialog
    if (deletingMessage != null) {
        val targetMsg = deletingMessage!!
        AlertDialog(
            onDismissRequest = { deletingMessage = null },
            containerColor = AdminSurface,
            title = {
                Text("Confirm Message Deletion", color = AdminCrimson, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Permanently purge this message from Firestore mesh storage?\n\n\"${targetMsg.content.take(60)}\"",
                    color = VhoqxethTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val msgId = targetMsg.messageId
                        deletingMessage = null
                        coroutineScope.launch {
                            chatRepository.deleteMessage(thread.id, msgId)
                            onLog("Purged message $msgId from thread ${thread.id}")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminCrimson)
                ) {
                    Text("Delete Message", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMessage = null }) {
                    Text("Cancel", color = VhoqxethTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun QuarantineContactsRoster(
    targetUser: UserProfile,
    contacts: List<UserContact>,
    chatRepository: MeshChatRepository,
    onOpenAddContact: () -> Unit,
    onLog: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val cleanHandle = targetUser.customUserId.trim().removePrefix("@").lowercase()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Connected Contacts (${contacts.size})",
                color = AdminAmber,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onOpenAddContact,
                colors = ButtonDefaults.buttonColors(containerColor = AdminAmber),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("quarantine_add_contact_button")
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Inject Contact", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No contacts registered for this account.", color = VhoqxethTextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(contacts, key = { it.contactUserId }) { contact ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AdminCardBg),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VhoqxethBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(VhoqxethPrimaryViolet),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.initials,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.displayName,
                                    color = VhoqxethTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "@${contact.contactUserId}",
                                    color = VhoqxethCyanBright,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        chatRepository.removeContact(cleanHandle, contact.contactUserId)
                                        onLog("Removed contact @${contact.contactUserId} from @$cleanHandle's roster")
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Contact",
                                    tint = AdminCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuarantineIdentityView(
    targetUser: UserProfile,
    adminRepository: AdminRepository,
    onPasswordResetRequest: () -> Unit,
    onUserUpdated: () -> Unit,
    onLog: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf(targetUser.displayName) }
    var firstName by remember { mutableStateOf(targetUser.firstName) }
    var lastName by remember { mutableStateOf(targetUser.lastName) }
    var dob by remember { mutableStateOf(targetUser.dateOfBirth) }
    var description by remember { mutableStateOf(targetUser.description) }
    var isSaving by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AdminCardBg),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AdminAmber.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("CRYPTOGRAPHIC ACCESS CREDENTIALS", color = AdminAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Password Hash: ${targetUser.passwordHash.take(16)}... (SHA-256)",
                        color = VhoqxethTextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Unique Salt: ${targetUser.passwordSalt.take(12)}...",
                        color = VhoqxethTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Bound IP Addresses: ${targetUser.associatedIps.joinToString(", ").ifBlank { "None mapped" }}",
                        color = VhoqxethCyanAccent,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onPasswordResetRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = AdminCrimson),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Override / Reset User Password", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("EDITABLE USER PROFILE ATTRIBUTES", color = AdminAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        item {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Visible Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AdminCardBg,
                    unfocusedContainerColor = AdminCardBg,
                    focusedBorderColor = AdminAmber,
                    unfocusedBorderColor = VhoqxethBorder,
                    focusedTextColor = VhoqxethTextPrimary,
                    unfocusedTextColor = VhoqxethTextPrimary
                )
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AdminCardBg,
                        unfocusedContainerColor = AdminCardBg,
                        focusedBorderColor = AdminAmber,
                        unfocusedBorderColor = VhoqxethBorder,
                        focusedTextColor = VhoqxethTextPrimary,
                        unfocusedTextColor = VhoqxethTextPrimary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AdminCardBg,
                        unfocusedContainerColor = AdminCardBg,
                        focusedBorderColor = AdminAmber,
                        unfocusedBorderColor = VhoqxethBorder,
                        focusedTextColor = VhoqxethTextPrimary,
                        unfocusedTextColor = VhoqxethTextPrimary
                    )
                )
            }
        }

        item {
            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                label = { Text("Date of Birth (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AdminCardBg,
                    unfocusedContainerColor = AdminCardBg,
                    focusedBorderColor = AdminAmber,
                    unfocusedBorderColor = VhoqxethBorder,
                    focusedTextColor = VhoqxethTextPrimary,
                    unfocusedTextColor = VhoqxethTextPrimary
                )
            )
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Bio / Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AdminCardBg,
                    unfocusedContainerColor = AdminCardBg,
                    focusedBorderColor = AdminAmber,
                    unfocusedBorderColor = VhoqxethBorder,
                    focusedTextColor = VhoqxethTextPrimary,
                    unfocusedTextColor = VhoqxethTextPrimary
                )
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = {
                    isSaving = true
                    val updated = targetUser.copy(
                        displayName = displayName.trim().ifBlank { targetUser.customUserId },
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        dateOfBirth = dob.trim(),
                        description = description.trim()
                    )
                    coroutineScope.launch {
                        adminRepository.updateUser(targetUser.customUserId, updated)
                        isSaving = false
                        onUserUpdated()
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = AdminAmber),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save Identity Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QuarantineAuditLogView(logs: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(logs) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AdminCardBg),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, VhoqxethBorder)
            ) {
                Text(
                    text = entry,
                    color = if (entry.startsWith("QUARANTINE")) AdminCrimson else VhoqxethTextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun QuarantineAddContactDialog(
    targetUser: UserProfile,
    chatRepository: MeshChatRepository,
    onDismiss: () -> Unit,
    onContactAdded: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var peerHandleInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        title = {
            Text("Inject Peer Contact to @${targetUser.customUserId}", color = AdminAmber, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Enter peer handle to establish reciprocal contact link directly:", color = VhoqxethTextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = peerHandleInput,
                    onValueChange = {
                        peerHandleInput = it
                        errorMessage = null
                    },
                    label = { Text("Peer Handle (@handle)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AdminCardBg,
                        unfocusedContainerColor = AdminCardBg,
                        focusedBorderColor = AdminAmber,
                        unfocusedBorderColor = VhoqxethBorder,
                        focusedTextColor = VhoqxethTextPrimary,
                        unfocusedTextColor = VhoqxethTextPrimary
                    )
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = AdminCrimson, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanPeer = peerHandleInput.trim().removePrefix("@").lowercase()
                    if (cleanPeer.isBlank()) {
                        errorMessage = "Peer handle cannot be blank."
                        return@Button
                    }
                    isAdding = true
                    coroutineScope.launch {
                        val peerProfileResult = chatRepository.searchUserByHandle(cleanPeer)
                        val peerProfile = peerProfileResult.getOrNull()
                        if (peerProfile == null) {
                            errorMessage = "User @$cleanPeer not found in database."
                            isAdding = false
                            return@launch
                        }

                        val addResult = chatRepository.addContact(targetUser, peerProfile)
                        isAdding = false
                        if (addResult.isSuccess) {
                            onContactAdded(cleanPeer)
                        } else {
                            errorMessage = addResult.exceptionOrNull()?.message ?: "Failed to add peer."
                        }
                    }
                },
                enabled = !isAdding,
                colors = ButtonDefaults.buttonColors(containerColor = AdminAmber)
            ) {
                if (isAdding) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Add Contact", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VhoqxethTextSecondary)
            }
        }
    )
}

@Composable
private fun QuarantinePasswordOverrideDialog(
    targetUser: UserProfile,
    adminRepository: AdminRepository,
    onDismiss: () -> Unit,
    onPasswordOverridden: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        title = {
            Text("Admin Password Override", color = AdminCrimson, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Assign a new password for @${targetUser.customUserId}. This generates a new cryptographic salt and SHA-256 hash:",
                    color = VhoqxethTextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = null
                    },
                    label = { Text("New Password (min 4 chars)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = VhoqxethTextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AdminCardBg,
                        unfocusedContainerColor = AdminCardBg,
                        focusedBorderColor = AdminCrimson,
                        unfocusedBorderColor = VhoqxethBorder,
                        focusedTextColor = VhoqxethTextPrimary,
                        unfocusedTextColor = VhoqxethTextPrimary
                    )
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = AdminCrimson, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.length < 4) {
                        errorMessage = "Password must be at least 4 characters."
                        return@Button
                    }
                    isSubmitting = true
                    val salt = PasswordSecurityUtil.generateSalt()
                    val hash = PasswordSecurityUtil.hashPassword(newPassword, salt)
                    val updated = targetUser.copy(passwordSalt = salt, passwordHash = hash)

                    coroutineScope.launch {
                        val res = adminRepository.updateUser(targetUser.customUserId, updated)
                        isSubmitting = false
                        if (res.isSuccess) {
                            onPasswordOverridden()
                        } else {
                            errorMessage = res.exceptionOrNull()?.message ?: "Failed to update password."
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = AdminCrimson)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Apply Override", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VhoqxethTextSecondary)
            }
        }
    )
}
