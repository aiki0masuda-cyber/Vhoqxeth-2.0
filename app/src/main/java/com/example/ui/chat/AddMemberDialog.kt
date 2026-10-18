package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MeshChatRepository
import com.example.model.ConversationThread
import com.example.model.UserProfile
import com.example.ui.theme.DavAccentSky
import com.example.ui.theme.DavBorder
import com.example.ui.theme.DavBrandRoyalBlue
import com.example.ui.theme.DavSurfaceElevated
import com.example.ui.theme.DavTextMuted
import com.example.ui.theme.DavTextPrimary
import com.example.ui.theme.DavTextSecondary
import kotlinx.coroutines.launch

@Composable
fun AddMemberDialog(
    thread: ConversationThread,
    currentUser: UserProfile,
    chatRepository: MeshChatRepository,
    onDismiss: () -> Unit,
    onMemberAdded: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var manualHandle by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val contactsFlow = remember(currentUser.customUserId) {
        chatRepository.observeContacts(currentUser.customUserId)
    }
    val allContacts by contactsFlow.collectAsState(initial = emptyList())

    // Filter contacts that are not yet participants in this thread
    val currentParticipants = thread.participantIds.map { it.lowercase() }
    val availableContacts = allContacts.filterNot {
        currentParticipants.contains(it.contactUserId.lowercase())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DavBrandRoyalBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = DavBrandRoyalBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Add to ${thread.name}",
                        color = DavTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Invite peers to this collaborative group",
                        color = DavTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (availableContacts.isNotEmpty()) {
                    Text(
                        text = "TAP CONTACT TO ADD IMMEDIATELY",
                        color = DavTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableContacts.forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DavSurfaceElevated)
                                    .border(1.dp, DavBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        isSubmitting = true
                                        errorMessage = null
                                        coroutineScope.launch {
                                            val res = chatRepository.addMemberToGroup(
                                                threadId = thread.id,
                                                newMemberHandle = contact.contactUserId,
                                                addedByProfile = currentUser
                                            )
                                            isSubmitting = false
                                            if (res.isSuccess) {
                                                successMessage = "Added @${contact.contactUserId}!"
                                                onMemberAdded()
                                            } else {
                                                errorMessage = res.exceptionOrNull()?.message ?: "Failed to add member"
                                            }
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(DavBrandRoyalBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.initials,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.displayName,
                                        color = DavTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "@${contact.contactUserId}",
                                        color = DavTextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "+ Add",
                                    color = DavBrandRoyalBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "All of your existing contacts are already members of this group.",
                        color = DavTextMuted,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "OR ENTER USER HANDLE",
                    color = DavTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                OutlinedTextField(
                    value = manualHandle,
                    onValueChange = {
                        manualHandle = it
                        errorMessage = null
                    },
                    placeholder = { Text("e.g. @rahul_sharma", color = DavTextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = DavSurfaceElevated,
                        focusedBorderColor = DavBrandRoyalBlue,
                        unfocusedBorderColor = DavBorder,
                        focusedTextColor = DavTextPrimary,
                        unfocusedTextColor = DavTextPrimary
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp
                    )
                }

                if (successMessage != null) {
                    Text(
                        text = successMessage!!,
                        color = Color(0xFF16A34A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            if (manualHandle.trim().isNotBlank()) {
                Button(
                    onClick = {
                        val handle = manualHandle.trim().removePrefix("@")
                        if (handle.isNotBlank()) {
                            isSubmitting = true
                            errorMessage = null
                            coroutineScope.launch {
                                val res = chatRepository.addMemberToGroup(
                                    threadId = thread.id,
                                    newMemberHandle = handle,
                                    addedByProfile = currentUser
                                )
                                isSubmitting = false
                                if (res.isSuccess) {
                                    manualHandle = ""
                                    successMessage = "Added @$handle!"
                                    onMemberAdded()
                                } else {
                                    errorMessage = res.exceptionOrNull()?.message ?: "Failed to add member"
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = DavBrandRoyalBlue)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Add Handle", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = DavTextSecondary)
            }
        }
    )
}
