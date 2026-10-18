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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.model.UserContact
import com.example.model.UserProfile
import com.example.ui.theme.DavAccentSky
import com.example.ui.theme.DavBorder
import com.example.ui.theme.DavBrandRoyalBlue
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
    initialPreselectedContact: String? = null,
    onDismiss: () -> Unit,
    onCircleCreated: (ConversationThread) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var groupName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Study Circle") }
    var grade by remember { mutableStateOf(currentUser.grade) }
    var section by remember { mutableStateOf(currentUser.section) }
    var subject by remember { mutableStateOf(currentUser.subject) }
    var description by remember { mutableStateOf("") }
    var manualHandleInput by remember { mutableStateOf("") }

    val selectedContacts = remember(initialPreselectedContact) {
        mutableStateListOf<String>().apply {
            if (!initialPreselectedContact.isNullOrBlank()) {
                val clean = initialPreselectedContact.trim().removePrefix("@").lowercase()
                if (clean.isNotBlank()) {
                    add(clean)
                }
            }
        }
    }

    // Live contacts for selecting members
    val contactsFlow = remember(currentUser.customUserId) {
        chatRepository.observeContacts(currentUser.customUserId)
    }
    val contactsList by contactsFlow.collectAsState(initial = emptyList())

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Study Circle", "Class Group", "Project Team", "Faculty Room")

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
                        .background(DavBrandRoyalBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = DavBrandRoyalBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Create Group",
                        color = DavTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DAV InfoExchange Collaborative Network",
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Pills
                Text(
                    text = "GROUP CATEGORY",
                    color = DavTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DavBrandRoyalBlue else DavSurfaceElevated)
                                .border(1.dp, if (isSelected) DavBrandRoyalBlue else DavBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.White else DavTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Group Name
                OutlinedTextField(
                    value = groupName,
                    onValueChange = {
                        groupName = it
                        errorMessage = null
                    },
                    label = { Text("Group / Circle Name *") },
                    placeholder = { Text("e.g. Grade 10-A Science Circle", color = DavTextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("circle_name_input"),
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

                // Grade & Section Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = grade,
                        onValueChange = { grade = it },
                        label = { Text("Class / Grade") },
                        placeholder = { Text("10", color = DavTextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("circle_grade_input"),
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

                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Section") },
                        placeholder = { Text("B", color = DavTextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("circle_section_input"),
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
                }

                // Subject / Topic
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Topic") },
                    placeholder = { Text("e.g. Physics & Chemistry", color = DavTextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("circle_subject_input"),
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

                HorizontalDivider(color = DavBorder, thickness = 0.5.dp)

                // Select Contacts to Add
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ADD CONTACTS TO GROUP",
                        color = DavTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    if (selectedContacts.isNotEmpty()) {
                        Text(
                            text = "${selectedContacts.size} selected",
                            color = DavAccentSky,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (contactsList.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        contactsList.forEach { contact ->
                            val isSelected = selectedContacts.contains(contact.contactUserId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE0F2FE) else DavSurfaceElevated)
                                    .border(1.dp, if (isSelected) DavAccentSky else DavBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isSelected) {
                                            selectedContacts.remove(contact.contactUserId)
                                        } else {
                                            selectedContacts.add(contact.contactUserId)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) DavAccentSky else DavBrandRoyalBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.initials,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
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
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = DavAccentSky,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No saved contacts yet. You can also invite users below by @handle.",
                        color = DavTextMuted,
                        fontSize = 11.sp
                    )
                }

                // Additional manual handles
                OutlinedTextField(
                    value = manualHandleInput,
                    onValueChange = { manualHandleInput = it },
                    label = { Text("Additional @handles (optional)") },
                    placeholder = { Text("e.g. @rahul @sneha (space separated)", color = DavTextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("circle_members_input"),
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

                // Optional description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Objective (Optional)") },
                    placeholder = { Text("Collaborative study discussions & class notes", color = DavTextMuted, fontSize = 12.sp) },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("circle_desc_input"),
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.trim().isBlank()) {
                        errorMessage = "Please enter a name for the group."
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null

                    val manualList = manualHandleInput.split(" ", ",")
                        .map { it.trim().removePrefix("@").lowercase() }
                        .filter { it.isNotBlank() }

                    val allInvited = (selectedContacts + manualList).distinct()

                    coroutineScope.launch {
                        val result = chatRepository.createStudyCircle(
                            creator = currentUser,
                            circleName = groupName.trim(),
                            grade = grade.trim(),
                            section = section.trim(),
                            subject = subject.trim(),
                            description = if (description.isNotBlank()) description.trim() else "$selectedCategory for DAV InfoExchange",
                            initialMembers = allInvited
                        )

                        isSubmitting = false
                        if (result.isSuccess) {
                            onCircleCreated(result.getOrThrow())
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to create group."
                        }
                    }
                },
                enabled = groupName.trim().isNotBlank() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DavBrandRoyalBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_create_circle_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create Group", fontWeight = FontWeight.Bold)
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
