package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MeshChatRepository
import com.example.model.ConversationThread
import com.example.model.UserProfile
import com.example.ui.theme.DavAccentSky
import com.example.ui.theme.DavBorder
import com.example.ui.theme.DavSurfaceCard
import com.example.ui.theme.DavSurfaceElevated
import com.example.ui.theme.DavTextMuted
import com.example.ui.theme.DavTextPrimary
import com.example.ui.theme.DavTextSecondary
import kotlinx.coroutines.launch

@Composable
fun CreateStudyCircleDialog(
    currentUser: UserProfile,
    chatRepository: MeshChatRepository,
    onDismiss: () -> Unit,
    onCircleCreated: (ConversationThread) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var circleName by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf(currentUser.grade) }
    var section by remember { mutableStateOf(currentUser.section) }
    var subject by remember { mutableStateOf(currentUser.subject) }
    var description by remember { mutableStateOf("") }
    var membersInput by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DavSurfaceCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = DavAccentSky,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Create Study Circle",
                    color = DavTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Form a dedicated scholastic circle for collaborative coursework, grade discussions, and subject study.",
                    color = DavTextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = circleName,
                    onValueChange = {
                        circleName = it
                        errorMessage = null
                    },
                    label = { Text("Study Circle Name *") },
                    placeholder = { Text("e.g. Grade 10-B Science Group", color = DavTextMuted) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("circle_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DavSurfaceElevated,
                        unfocusedContainerColor = DavSurfaceElevated,
                        focusedBorderColor = DavAccentSky,
                        unfocusedBorderColor = DavBorder,
                        focusedTextColor = DavTextPrimary,
                        unfocusedTextColor = DavTextPrimary
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = grade,
                        onValueChange = { grade = it },
                        label = { Text("Class / Grade") },
                        placeholder = { Text("10", color = DavTextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("circle_grade_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DavSurfaceElevated,
                            unfocusedContainerColor = DavSurfaceElevated,
                            focusedBorderColor = DavAccentSky,
                            unfocusedBorderColor = DavBorder,
                            focusedTextColor = DavTextPrimary,
                            unfocusedTextColor = DavTextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Section") },
                        placeholder = { Text("B", color = DavTextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("circle_section_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DavSurfaceElevated,
                            unfocusedContainerColor = DavSurfaceElevated,
                            focusedBorderColor = DavAccentSky,
                            unfocusedBorderColor = DavBorder,
                            focusedTextColor = DavTextPrimary,
                            unfocusedTextColor = DavTextPrimary
                        )
                    )
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Topic") },
                    placeholder = { Text("e.g. Mathematics, Science", color = DavTextMuted) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("circle_subject_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DavSurfaceElevated,
                        unfocusedContainerColor = DavSurfaceElevated,
                        focusedBorderColor = DavAccentSky,
                        unfocusedBorderColor = DavBorder,
                        focusedTextColor = DavTextPrimary,
                        unfocusedTextColor = DavTextPrimary
                    )
                )

                OutlinedTextField(
                    value = membersInput,
                    onValueChange = { membersInput = it },
                    label = { Text("Invite Classmates (@handle, separated by space)") },
                    placeholder = { Text("@rahul @sneha", color = DavTextMuted) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("circle_members_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DavSurfaceElevated,
                        unfocusedContainerColor = DavSurfaceElevated,
                        focusedBorderColor = DavAccentSky,
                        unfocusedBorderColor = DavBorder,
                        focusedTextColor = DavTextPrimary,
                        unfocusedTextColor = DavTextPrimary
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Circle Description (Optional)") },
                    placeholder = { Text("Coursework questions & homework exchange", color = DavTextMuted) },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("circle_desc_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DavSurfaceElevated,
                        unfocusedContainerColor = DavSurfaceElevated,
                        focusedBorderColor = DavAccentSky,
                        unfocusedBorderColor = DavBorder,
                        focusedTextColor = DavTextPrimary,
                        unfocusedTextColor = DavTextPrimary
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (circleName.trim().isBlank()) {
                        errorMessage = "Please enter a name for the study circle."
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null

                    val invited = membersInput.split(" ", ",")
                        .map { it.trim().removePrefix("@").lowercase() }
                        .filter { it.isNotBlank() }

                    coroutineScope.launch {
                        val result = chatRepository.createStudyCircle(
                            creator = currentUser,
                            circleName = circleName.trim(),
                            grade = grade.trim(),
                            section = section.trim(),
                            subject = subject.trim(),
                            description = description.trim(),
                            initialMembers = invited
                        )

                        isSubmitting = false
                        if (result.isSuccess) {
                            onCircleCreated(result.getOrThrow())
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to create study circle."
                        }
                    }
                },
                enabled = circleName.trim().isNotBlank() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DavAccentSky,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_create_circle_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create Circle", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DavTextSecondary)
            }
        }
    )
}
