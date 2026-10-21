package com.example.ui.chat

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.AiImageGeneratorEngine
import com.example.data.MeshChatRepository
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

@Composable
fun GroupInfoSettingsSheet(
    initialThread: ConversationThread,
    currentUser: UserProfile,
    chatRepository: MeshChatRepository,
    onAddMembersRequested: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Observe live thread updates
    val liveThreadState by chatRepository.observeThread(initialThread.id).collectAsState(initial = initialThread)
    val thread = liveThreadState ?: initialThread

    val currentHandle = currentUser.customUserId.trim().removePrefix("@").lowercase()
    val isUserAdmin = thread.isAdmin(currentHandle)
    val isUserCreator = thread.isCreator(currentHandle)

    // Form fields
    var groupName by remember(thread.name) { mutableStateOf(thread.name) }
    var subject by remember(thread.subject) { mutableStateOf(thread.subject) }
    var grade by remember(thread.grade) { mutableStateOf(thread.grade) }
    var section by remember(thread.section) { mutableStateOf(thread.section) }
    var description by remember(thread.description) { mutableStateOf(thread.description) }

    var onlyAdminsCanChat by remember(thread.onlyAdminsCanChat) { mutableStateOf(thread.onlyAdminsCanChat) }
    var onlyAdminsCanAddMembers by remember(thread.onlyAdminsCanAddMembers) { mutableStateOf(thread.onlyAdminsCanAddMembers) }

    var isSavingDetails by remember { mutableStateOf(false) }
    var isSavingPermissions by remember { mutableStateOf(false) }
    var showAiAvatarDialog by remember { mutableStateOf(false) }
    var memberActionMenuFor by remember { mutableStateOf<String?>(null) }
    var memberToRemove by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .background(DavCanvasLight)
            .navigationBarsPadding()
            .imePadding()
            .testTag("group_info_settings_sheet")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DavAccentSky.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = DavBrandRoyalBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Group Info & Settings",
                        color = DavTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isUserAdmin) "Administrator Controls Active" else "Member View",
                        color = if (isUserAdmin) DavBrandRoyalBlue else DavTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_group_info_sheet_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = DavTextSecondary
                )
            }
        }

        HorizontalDivider(color = DavBorder, thickness = 0.5.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group Avatar Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, DavBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar display
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DavAccentSky)
                            .border(2.dp, DavBrandRoyalBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!thread.avatarUri.isNullOrBlank()) {
                            AsyncImage(
                                model = thread.avatarUri,
                                contentDescription = "Group Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = thread.avatarInitials.ifBlank { "GP" },
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = thread.name,
                            color = DavTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${thread.participantIds.size} Members • ${if (thread.subject.isNotBlank()) thread.subject else "Study Circle"}",
                            color = DavTextSecondary,
                            fontSize = 12.sp
                        )

                        if (isUserAdmin) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showAiAvatarDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DavAccentSky.copy(alpha = 0.15f),
                                    contentColor = DavBrandRoyalBlue
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("generate_ai_avatar_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Generate AI Avatar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Editable or View-Only Group Details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, DavBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "GROUP DETAILS",
                            color = DavBrandRoyalBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        if (!isUserAdmin) {
                            Text(
                                text = "Admin Managed",
                                color = DavTextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        enabled = isUserAdmin,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DavCanvasLight,
                            unfocusedContainerColor = DavCanvasLight,
                            focusedBorderColor = DavBrandRoyalBlue,
                            unfocusedBorderColor = DavBorder,
                            focusedTextColor = DavTextPrimary,
                            unfocusedTextColor = DavTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_name_input_field")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Subject") },
                            enabled = isUserAdmin,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DavCanvasLight,
                                unfocusedContainerColor = DavCanvasLight,
                                focusedBorderColor = DavBrandRoyalBlue,
                                unfocusedBorderColor = DavBorder,
                                focusedTextColor = DavTextPrimary,
                                unfocusedTextColor = DavTextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = grade,
                            onValueChange = { grade = it },
                            label = { Text("Class / Grade") },
                            enabled = isUserAdmin,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DavCanvasLight,
                                unfocusedContainerColor = DavCanvasLight,
                                focusedBorderColor = DavBrandRoyalBlue,
                                unfocusedBorderColor = DavBorder,
                                focusedTextColor = DavTextPrimary,
                                unfocusedTextColor = DavTextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = section,
                            onValueChange = { section = it },
                            label = { Text("Section") },
                            enabled = isUserAdmin,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DavCanvasLight,
                                unfocusedContainerColor = DavCanvasLight,
                                focusedBorderColor = DavBrandRoyalBlue,
                                unfocusedBorderColor = DavBorder,
                                focusedTextColor = DavTextPrimary,
                                unfocusedTextColor = DavTextPrimary
                            ),
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Academic Objectives / Description") },
                        enabled = isUserAdmin,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DavCanvasLight,
                            unfocusedContainerColor = DavCanvasLight,
                            focusedBorderColor = DavBrandRoyalBlue,
                            unfocusedBorderColor = DavBorder,
                            focusedTextColor = DavTextPrimary,
                            unfocusedTextColor = DavTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isUserAdmin) {
                        Button(
                            onClick = {
                                if (groupName.trim().isBlank()) {
                                    Toast.makeText(context, "Group name cannot be blank", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isSavingDetails = true
                                coroutineScope.launch {
                                    val res = chatRepository.updateGroupSettings(
                                        threadId = thread.id,
                                        name = groupName,
                                        description = description,
                                        subject = subject,
                                        grade = grade,
                                        section = section
                                    )
                                    isSavingDetails = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Group details updated", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isSavingDetails && groupName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DavBrandRoyalBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_group_details_button")
                        ) {
                            if (isSavingDetails) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Group Details", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Group Permissions Toggles (Admins only)
            if (isUserAdmin) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, DavBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = DavBrandRoyalBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GROUP PERMISSIONS (ADMINS ONLY)",
                                color = DavBrandRoyalBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Toggle 1: Who can chat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Send Messages: Admins Only",
                                    color = DavTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (onlyAdminsCanChat) "Only group creator and co-admins can send messages" else "All group members can participate and chat",
                                    color = DavTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = onlyAdminsCanChat,
                                onCheckedChange = { checked ->
                                    onlyAdminsCanChat = checked
                                    coroutineScope.launch {
                                        chatRepository.updateGroupPermissions(thread.id, checked, onlyAdminsCanAddMembers)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DavBrandRoyalBlue
                                ),
                                modifier = Modifier.testTag("admin_only_chat_toggle")
                            )
                        }

                        HorizontalDivider(color = DavBorder, thickness = 0.5.dp)

                        // Toggle 2: Who can add users
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Add Participants: Admins Only",
                                    color = DavTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (onlyAdminsCanAddMembers) "Only group creator and co-admins can add new participants" else "All group members can invite contacts to this group",
                                    color = DavTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = onlyAdminsCanAddMembers,
                                onCheckedChange = { checked ->
                                    onlyAdminsCanAddMembers = checked
                                    coroutineScope.launch {
                                        chatRepository.updateGroupPermissions(thread.id, onlyAdminsCanChat, checked)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DavBrandRoyalBlue
                                ),
                                modifier = Modifier.testTag("admin_only_add_members_toggle")
                            )
                        }
                    }
                }
            } else if (thread.onlyAdminsCanChat || thread.onlyAdminsCanAddMembers) {
                // Non-admin status notification card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DavSurfaceElevated)
                        .border(1.dp, DavBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = DavBrandRoyalBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Group Governance Policy Active",
                                color = DavBrandRoyalBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (thread.onlyAdminsCanChat) {
                            Text(
                                text = "• Announcement Mode: Only group admins can send messages",
                                color = DavTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        if (thread.onlyAdminsCanAddMembers) {
                            Text(
                                text = "• Membership: Only group admins can add new participants",
                                color = DavTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Participants & Admin Roles Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, DavBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "PARTICIPANTS (${thread.participantIds.size})",
                            color = DavBrandRoyalBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        if (thread.canUserAddMembers(currentHandle)) {
                            OutlinedButton(
                                onClick = onAddMembersRequested,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("group_info_add_member_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add Member",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Participant roster
                    thread.participantIds.forEach { participant ->
                        val cleanParticipant = participant.trim().removePrefix("@").lowercase()
                        val isCreator = cleanParticipant == thread.creatorId.trim().removePrefix("@").lowercase()
                        val isAdmin = thread.isAdmin(cleanParticipant)
                        val isSelf = cleanParticipant == currentHandle

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DavSurfaceElevated)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isCreator) DavBrandRoyalBlue else if (isAdmin) DavAccentSky else Color(0xFF94A3B8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cleanParticipant.take(2).uppercase(),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "@$cleanParticipant",
                                        color = DavTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isSelf) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(You)",
                                            color = DavTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (isCreator) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(DavBrandRoyalBlue)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "CREATOR",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (isAdmin) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(DavAccentSky)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ADMIN",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFE2E8F0))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "MEMBER",
                                                color = DavTextSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Admin action 3-dot menu
                            if (isUserAdmin && !isSelf && !isCreator) {
                                Box {
                                    IconButton(
                                        onClick = { memberActionMenuFor = cleanParticipant },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Member Actions",
                                            tint = DavTextSecondary
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = memberActionMenuFor == cleanParticipant,
                                        onDismissRequest = { memberActionMenuFor = null }
                                    ) {
                                        if (!isAdmin) {
                                            DropdownMenuItem(
                                                text = { Text("Make Co-Admin") },
                                                leadingIcon = {
                                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = DavBrandRoyalBlue)
                                                },
                                                onClick = {
                                                    memberActionMenuFor = null
                                                    coroutineScope.launch {
                                                        chatRepository.setGroupMemberAdmin(thread.id, cleanParticipant, true)
                                                        Toast.makeText(context, "@$cleanParticipant promoted to Co-Admin", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        } else {
                                            DropdownMenuItem(
                                                text = { Text("Dismiss as Admin") },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Shield, contentDescription = null, tint = DavTextMuted)
                                                },
                                                onClick = {
                                                    memberActionMenuFor = null
                                                    coroutineScope.launch {
                                                        chatRepository.setGroupMemberAdmin(thread.id, cleanParticipant, false)
                                                        Toast.makeText(context, "@$cleanParticipant dismissed as Admin", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }

                                        DropdownMenuItem(
                                            text = { Text("Remove from Group", color = Color(0xFFDC2626)) },
                                            leadingIcon = {
                                                Icon(Icons.Default.PersonRemove, contentDescription = null, tint = Color(0xFFDC2626))
                                            },
                                            onClick = {
                                                memberActionMenuFor = null
                                                memberToRemove = cleanParticipant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // AI Avatar Creator Dialog
    if (showAiAvatarDialog) {
        val suggestedAvatarPrompt = "Prestigious academic emblem crest for \"${thread.name}\" ${if (thread.subject.isNotBlank()) "focusing on ${thread.subject}" else ""}, featuring Vedic lamp of learning, knowledge shield, scholastic laurel wreath, high resolution clean vector emblem"

        AiGenerateImageDialog(
            initialPrompt = suggestedAvatarPrompt,
            title = "Generate Group Avatar Crest",
            confirmButtonLabel = "Set as Group Avatar",
            onDismiss = { showAiAvatarDialog = false },
            onImageAccepted = { bitmap, _ ->
                coroutineScope.launch {
                    val uri = AiImageGeneratorEngine.saveBitmapToLocalCache(context, bitmap, prefix = "group_avatar_${thread.id}")
                    chatRepository.updateGroupAvatar(thread.id, uri.toString())
                    Toast.makeText(context, "Group avatar updated!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Remove Member Confirmation Dialog
    if (memberToRemove != null) {
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Remove Member") },
            text = { Text("Are you sure you want to remove @${memberToRemove} from ${thread.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        val m = memberToRemove!!
                        memberToRemove = null
                        coroutineScope.launch {
                            chatRepository.removeMemberFromGroup(thread.id, m)
                            Toast.makeText(context, "@$m removed from group", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { memberToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
