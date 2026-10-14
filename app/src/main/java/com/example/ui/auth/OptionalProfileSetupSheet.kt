package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthRepository
import com.example.model.UserProfile
import com.example.ui.theme.VhoqxethBorder
import com.example.ui.theme.VhoqxethBorderLightStrong
import com.example.ui.theme.VhoqxethIndigo600
import com.example.ui.theme.VhoqxethSlate900
import com.example.ui.theme.VhoqxethSurface
import com.example.ui.theme.VhoqxethTextMuted
import com.example.ui.theme.VhoqxethTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionalProfileSetupSheet(
    currentUser: UserProfile,
    authRepository: AuthRepository,
    onSaved: (UserProfile) -> Unit,
    onSkipped: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val coroutineScope = rememberCoroutineScope()
    var firstName by remember { mutableStateOf(currentUser.firstName) }
    var lastName by remember { mutableStateOf(currentUser.lastName) }
    var dateOfBirth by remember { mutableStateOf(currentUser.dateOfBirth) }
    var description by remember { mutableStateOf(currentUser.description) }
    var isSaving by remember { mutableStateOf(false) }
    var isSkipping by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onSkipped,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(VhoqxethBorder)
            )
        },
        modifier = Modifier.testTag("optional_profile_setup_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Icon Badge
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(VhoqxethIndigo600.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = VhoqxethIndigo600,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Personalize Your Mesh Profile",
                color = VhoqxethSlate900,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Welcome, ${currentUser.displayName}! All fields below are completely optional. Fill any details you'd like to share, or skip to enter chats immediately.",
                color = VhoqxethTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Two-column First and Last Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name (Optional)") },
                    placeholder = { Text("e.g. Alex", color = VhoqxethTextMuted) },
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
                        .weight(1f)
                        .testTag("setup_first_name_input")
                )

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name (Optional)") },
                    placeholder = { Text("e.g. Vance", color = VhoqxethTextMuted) },
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
                        .weight(1f)
                        .testTag("setup_last_name_input")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date of Birth Field
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { dateOfBirth = it },
                label = { Text("Date of Birth (Optional)") },
                placeholder = { Text("e.g. 1998-05-14", color = VhoqxethTextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = null,
                        tint = VhoqxethIndigo600,
                        modifier = Modifier.size(20.dp)
                    )
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
                    .testTag("setup_dob_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Description / Bio Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("About / Description (Optional)") },
                placeholder = { Text("Write a short bio, status or notes...", color = VhoqxethTextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = VhoqxethIndigo600,
                        modifier = Modifier.size(20.dp)
                    )
                },
                minLines = 3,
                maxLines = 5,
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
                    .testTag("setup_description_input")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Actions: Save Details & Skip for Now
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (isSaving || isSkipping) return@Button
                        isSaving = true
                        coroutineScope.launch {
                            val res = authRepository.updateOptionalProfileDetails(
                                userId = currentUser.customUserId,
                                firstName = firstName,
                                lastName = lastName,
                                dateOfBirth = dateOfBirth,
                                description = description
                            )
                            isSaving = false
                            if (res.isSuccess) {
                                onSaved(res.getOrThrow())
                            } else {
                                // Graceful fallback
                                onSaved(
                                    currentUser.copy(
                                        firstName = firstName.trim(),
                                        lastName = lastName.trim(),
                                        dateOfBirth = dateOfBirth.trim(),
                                        description = description.trim(),
                                        hasCompletedOptionalSetup = true
                                    )
                                )
                            }
                        }
                    },
                    enabled = !isSaving && !isSkipping,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VhoqxethIndigo600,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_optional_profile_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving Details...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Enter Chats", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        if (isSaving || isSkipping) return@OutlinedButton
                        isSkipping = true
                        coroutineScope.launch {
                            authRepository.skipOptionalProfileDetails(currentUser.customUserId)
                            isSkipping = false
                            onSkipped()
                        }
                    },
                    enabled = !isSaving && !isSkipping,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = VhoqxethSlate900
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("skip_optional_profile_button")
                ) {
                    Text("Skip for Now", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
