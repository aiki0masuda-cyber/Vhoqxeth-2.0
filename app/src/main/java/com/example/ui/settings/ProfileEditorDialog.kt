package com.example.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.AuthRepository
import com.example.model.UserProfile
import com.example.ui.theme.VhoqxethBorder
import com.example.ui.theme.VhoqxethBorderLightStrong
import com.example.ui.theme.VhoqxethCyanAccent
import com.example.ui.theme.VhoqxethIndigo600
import com.example.ui.theme.VhoqxethSlate900
import com.example.ui.theme.VhoqxethTextMuted
import com.example.ui.theme.VhoqxethTextSecondary
import kotlinx.coroutines.launch

@Composable
fun ProfileEditorDialog(
    currentUser: UserProfile,
    authRepository: AuthRepository,
    onDismiss: () -> Unit,
    onProfileUpdated: (UserProfile) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf(currentUser.displayName) }
    var firstName by remember { mutableStateOf(currentUser.firstName) }
    var lastName by remember { mutableStateOf(currentUser.lastName) }
    var dateOfBirth by remember { mutableStateOf(currentUser.dateOfBirth) }
    var description by remember { mutableStateOf(currentUser.description) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(VhoqxethIndigo600.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = VhoqxethIndigo600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Edit Profile Details",
                        color = VhoqxethSlate900,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "IDENTITY MANAGEMENT",
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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Locked Handle Notice
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, VhoqxethBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked handle",
                            tint = VhoqxethTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Handle / User ID (Permanent)",
                                color = VhoqxethTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@${currentUser.customUserId}",
                                color = VhoqxethSlate900,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Visible Display Name
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Visible Name (Required)") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = VhoqxethIndigo600,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
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
                        .testTag("edit_display_name_input")
                )

                // First and Last Name row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = VhoqxethIndigo600,
                            unfocusedBorderColor = VhoqxethBorder,
                            focusedTextColor = VhoqxethSlate900,
                            unfocusedTextColor = VhoqxethSlate900
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_first_name_input")
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = VhoqxethIndigo600,
                            unfocusedBorderColor = VhoqxethBorder,
                            focusedTextColor = VhoqxethSlate900,
                            unfocusedTextColor = VhoqxethSlate900
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_last_name_input")
                    )
                }

                // Date of Birth
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    label = { Text("Date of Birth (Optional)") },
                    placeholder = { Text("YYYY-MM-DD", color = VhoqxethTextMuted) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = null,
                            tint = VhoqxethIndigo600,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
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
                        .testTag("edit_dob_input")
                )

                // Description / Bio
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Bio") },
                    placeholder = { Text("Short bio or status...", color = VhoqxethTextMuted) },
                    minLines = 2,
                    maxLines = 4,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = VhoqxethIndigo600,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
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
                        .testTag("edit_description_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (displayName.trim().isBlank()) {
                        errorMessage = "Visible Name cannot be empty"
                        return@Button
                    }
                    isSaving = true
                    errorMessage = null
                    coroutineScope.launch {
                        // 1. Update visible name if changed
                        if (displayName.trim() != currentUser.displayName) {
                            authRepository.updateVisibleName(currentUser.customUserId, displayName.trim())
                        }
                        // 2. Update optional details
                        val res = authRepository.updateOptionalProfileDetails(
                            userId = currentUser.customUserId,
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            dateOfBirth = dateOfBirth.trim(),
                            description = description.trim()
                        )
                        isSaving = false
                        if (res.isSuccess) {
                            val updated = res.getOrThrow().copy(displayName = displayName.trim())
                            onProfileUpdated(updated)
                            onDismiss()
                        } else {
                            errorMessage = res.exceptionOrNull()?.message ?: "Failed to update profile"
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VhoqxethIndigo600,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_profile_edits_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saving...")
                } else {
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = VhoqxethSlate900
                ),
                modifier = Modifier.testTag("cancel_profile_edits_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
