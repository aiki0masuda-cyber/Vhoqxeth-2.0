package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MeshChatRepository
import com.example.model.ConversationThread
import com.example.model.UserProfile
import com.example.ui.theme.VhoqxethBorder
import com.example.ui.theme.VhoqxethBorderLightStrong
import com.example.ui.theme.VhoqxethCyanAccent
import com.example.ui.theme.VhoqxethIndigo600
import com.example.ui.theme.VhoqxethPrimaryViolet
import com.example.ui.theme.VhoqxethSlate900
import com.example.ui.theme.VhoqxethSurfaceElevated
import com.example.ui.theme.VhoqxethTextMuted
import com.example.ui.theme.VhoqxethTextPrimary
import com.example.ui.theme.VhoqxethTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AddContactDialog(
    currentUser: UserProfile,
    chatRepository: MeshChatRepository,
    onDismiss: () -> Unit,
    onContactAdded: (ConversationThread) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchHandle by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var foundProfile by remember { mutableStateOf<UserProfile?>(null) }
    var searchAttempted by remember { mutableStateOf(false) }
    var isAdding by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    val currentHandle = currentUser.customUserId.trim().removePrefix("@").lowercase()

    fun triggerSearch(query: String) {
        val sanitized = query.trim().removePrefix("@").lowercase()
        searchJob?.cancel()
        actionError = null

        if (sanitized.isBlank()) {
            isSearching = false
            foundProfile = null
            searchAttempted = false
            return
        }

        if (sanitized == currentHandle) {
            isSearching = false
            foundProfile = null
            searchAttempted = true
            actionError = "You cannot add your own User ID."
            return
        }

        isSearching = true
        searchAttempted = false

        searchJob = coroutineScope.launch {
            delay(350L) // debounce
            val result = chatRepository.searchUserByHandle(sanitized)
            isSearching = false
            searchAttempted = true
            if (result.isSuccess) {
                foundProfile = result.getOrNull()
            } else {
                foundProfile = null
                actionError = result.exceptionOrNull()?.message ?: "Search failed"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VhoqxethIndigo600.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = VhoqxethIndigo600,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Add Mesh Contact",
                        color = VhoqxethSlate900,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DISCOVER BY USER ID",
                        color = VhoqxethIndigo600,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Enter a peer's unique User ID handle to establish an encrypted two-way conversation channel:",
                    color = VhoqxethTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // Search Input with real-time feedback
                OutlinedTextField(
                    value = searchHandle,
                    onValueChange = {
                        searchHandle = it
                        triggerSearch(it)
                    },
                    placeholder = {
                        Text("e.g. alice, @bob_mesh", color = VhoqxethTextMuted)
                    },
                    leadingIcon = {
                        Text(
                            text = "@",
                            color = VhoqxethIndigo600,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 12.dp, end = 2.dp)
                        )
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = VhoqxethIndigo600,
                                strokeWidth = 2.dp
                            )
                        } else if (searchHandle.isNotBlank()) {
                            IconButton(onClick = {
                                searchHandle = ""
                                triggerSearch("")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = VhoqxethTextMuted
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = VhoqxethIndigo600,
                        unfocusedBorderColor = VhoqxethBorder,
                        focusedTextColor = VhoqxethSlate900,
                        unfocusedTextColor = VhoqxethSlate900
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_user_id_input")
                )

                // Error Message if any
                if (actionError != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = actionError ?: "",
                            color = Color(0xFFB91C1C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Found User Profile Preview Card
                AnimatedVisibility(
                    visible = foundProfile != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    foundProfile?.let { target ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(1.dp, VhoqxethBorderLightStrong, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                                .testTag("found_user_preview_card")
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(VhoqxethIndigo600),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = target.displayName.take(2).uppercase().ifBlank { "VQ" },
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = target.displayName,
                                                color = VhoqxethSlate900,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Verified User",
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = "@${target.customUserId}",
                                            color = VhoqxethIndigo600,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (target.currentIp.isNotBlank()) {
                                    Text(
                                        text = "Mesh Network IP: ${target.currentIp}",
                                        color = VhoqxethTextMuted,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (isAdding) return@Button
                                        isAdding = true
                                        actionError = null
                                        coroutineScope.launch {
                                            val addResult = chatRepository.addContact(currentUser, target)
                                            isAdding = false
                                            if (addResult.isSuccess) {
                                                onContactAdded(addResult.getOrThrow())
                                            } else {
                                                actionError = addResult.exceptionOrNull()?.message ?: "Failed to add contact"
                                            }
                                        }
                                    },
                                    enabled = !isAdding,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VhoqxethIndigo600,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_add_contact_button")
                                ) {
                                    if (isAdding) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Adding to Mesh...")
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add to Contacts & Start Chat")
                                    }
                                }
                            }
                        }
                    }
                }

                // Empty / Not Found State
                if (searchAttempted && foundProfile == null && !isSearching && searchHandle.isNotBlank() && actionError == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, VhoqxethBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = VhoqxethTextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No registered user found with ID @${searchHandle.trim().removePrefix("@")}",
                                color = VhoqxethTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Verify the spelling or have them register on the network first.",
                                color = VhoqxethTextMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = VhoqxethSlate900
                ),
                modifier = Modifier.testTag("cancel_add_contact_button")
            ) {
                Text("Close")
            }
        }
    )
}
