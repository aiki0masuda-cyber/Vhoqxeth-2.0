package com.example.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthRepository
import com.example.data.DeviceIpManager
import com.example.model.RegisteredIpEntry
import com.example.model.UserProfile
import com.example.ui.theme.VhoqxethBorder
import com.example.ui.theme.VhoqxethCyanAccent
import com.example.ui.theme.VhoqxethCyanBright
import com.example.ui.theme.VhoqxethDarkBg
import com.example.ui.theme.VhoqxethOnlineGreen
import com.example.ui.theme.VhoqxethPrimaryViolet
import com.example.ui.theme.VhoqxethPrimaryVioletBright
import com.example.ui.theme.VhoqxethSurface
import com.example.ui.theme.VhoqxethSurfaceElevated
import com.example.ui.theme.VhoqxethTextMuted
import com.example.ui.theme.VhoqxethTextPrimary
import com.example.ui.theme.VhoqxethTextSecondary
import kotlinx.coroutines.launch

sealed class AuthScreenStep {
    object Checking : AuthScreenStep()
    data class Recognized(val ip: String, val entry: RegisteredIpEntry) : AuthScreenStep()
    data class Unrecognized(val ip: String) : AuthScreenStep()
    data class RegisterForm(val ip: String) : AuthScreenStep()
    data class LinkExisting(val ip: String) : AuthScreenStep()
}

@Composable
fun AuthGatewayScreen(
    onAuthenticated: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }
    val ipManager = remember { DeviceIpManager(context) }

    var step by remember { mutableStateOf<AuthScreenStep>(AuthScreenStep.Checking) }
    var detectedIp by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun checkDeviceAndAuth() {
        isLoading = true
        errorMessage = null
        step = AuthScreenStep.Checking

        coroutineScope.launch {
            // 1. Resolve Device Network IP
            val ip = ipManager.resolveDeviceIp()
            detectedIp = ip

            // 2. Check if device IP is already registered in Firestore database
            val checkIpResult = authRepository.checkIpRegistration(ip)
            val ipEntry = checkIpResult.getOrNull()

            if (ipEntry != null) {
                // IP is recognized in database! Present quick continue card
                isLoading = false
                step = AuthScreenStep.Recognized(ip, ipEntry)
            } else {
                // IP not registered, prompt decision to create account or login
                isLoading = false
                step = AuthScreenStep.Unrecognized(ip)
            }
        }
    }

    LaunchedEffect(Unit) {
        checkDeviceAndAuth()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VhoqxethDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("auth_gateway_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Branding Header
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(VhoqxethPrimaryViolet, VhoqxethCyanAccent)
                        )
                    )
                    .border(2.dp, VhoqxethPrimaryVioletBright, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Vhoqxeth Vault",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Vhoqxeth",
                color = VhoqxethTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.5.sp,
                modifier = Modifier.testTag("auth_brand_title")
            )

            Text(
                text = "IP-BASED PROTOCOL ACCESS",
                color = VhoqxethCyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // IP Status Banner
            detectedIp?.let { ip ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VhoqxethSurface)
                        .border(1.dp, VhoqxethBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("detected_ip_pill"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (step is AuthScreenStep.Recognized) VhoqxethOnlineGreen else VhoqxethCyanAccent
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device IP: $ip",
                        color = VhoqxethTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (step is AuthScreenStep.Recognized) "• REGISTERED" else "• NEW NETWORK",
                        color = if (step is AuthScreenStep.Recognized) VhoqxethOnlineGreen else VhoqxethCyanBright,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error display
            errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3B1E2B))
                        .border(1.dp, Color(0xFFEF5350), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = error,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Step Content
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "AuthStepTransition"
            ) { currentStep ->
                when (currentStep) {
                    is AuthScreenStep.Checking -> {
                        CheckingStepContent()
                    }
                    is AuthScreenStep.Recognized -> {
                        RecognizedStepContent(
                            entry = currentStep.entry,
                            ip = currentStep.ip,
                            isLoading = isLoading,
                            onContinue = {
                                isLoading = true
                                coroutineScope.launch {
                                    val profile = authRepository.getUserProfile(currentStep.entry.customUserId).getOrNull()
                                        ?: UserProfile(
                                            userId = currentStep.entry.customUserId,
                                            displayName = currentStep.entry.displayName,
                                            customUserId = currentStep.entry.customUserId,
                                            currentIp = currentStep.ip
                                        )
                                    isLoading = false
                                    onAuthenticated(profile)
                                }
                            },
                            onSwitchAccount = {
                                step = AuthScreenStep.Unrecognized(currentStep.ip)
                            }
                        )
                    }
                    is AuthScreenStep.Unrecognized -> {
                        UnrecognizedStepContent(
                            ip = currentStep.ip,
                            onCreateAccount = {
                                step = AuthScreenStep.RegisterForm(currentStep.ip)
                            },
                            onSignInExisting = {
                                step = AuthScreenStep.LinkExisting(currentStep.ip)
                            }
                        )
                    }
                    is AuthScreenStep.RegisterForm -> {
                        RegisterFormContent(
                            ip = currentStep.ip,
                            isLoading = isLoading,
                            onBack = {
                                step = AuthScreenStep.Unrecognized(currentStep.ip)
                            },
                            onSubmit = { name, customId ->
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val regResult = authRepository.registerUserProfile(
                                        displayName = name,
                                        customUserId = customId,
                                        ip = currentStep.ip
                                    )
                                    isLoading = false
                                    if (regResult.isSuccess) {
                                        onAuthenticated(regResult.getOrNull()!!)
                                    } else {
                                        errorMessage = regResult.exceptionOrNull()?.localizedMessage
                                            ?: "Failed to register profile on database"
                                    }
                                }
                            }
                        )
                    }
                    is AuthScreenStep.LinkExisting -> {
                        LinkExistingContent(
                            ip = currentStep.ip,
                            isLoading = isLoading,
                            onBack = {
                                step = AuthScreenStep.Unrecognized(currentStep.ip)
                            },
                            onLink = { customId ->
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val linkResult = authRepository.linkIpToUserProfile(
                                        customUserId = customId,
                                        ip = currentStep.ip
                                    )
                                    isLoading = false
                                    if (linkResult.isSuccess) {
                                        onAuthenticated(linkResult.getOrNull()!!)
                                    } else {
                                        errorMessage = linkResult.exceptionOrNull()?.localizedMessage
                                            ?: "User not found or failed to link IP"
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CheckingStepContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VhoqxethSurface)
            .border(1.dp, VhoqxethBorder, RoundedCornerShape(16.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = VhoqxethCyanAccent,
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Analyzing Device IP",
            color = VhoqxethTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Detecting network IP and checking database registration...",
            color = VhoqxethTextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecognizedStepContent(
    entry: RegisteredIpEntry,
    ip: String,
    isLoading: Boolean,
    onContinue: () -> Unit,
    onSwitchAccount: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VhoqxethSurface)
            .border(1.dp, VhoqxethCyanAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(22.dp)
            .testTag("recognized_device_card"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(VhoqxethOnlineGreen.copy(alpha = 0.15f))
                .border(1.5.dp, VhoqxethOnlineGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Recognized",
                tint = VhoqxethOnlineGreen,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Device Recognized",
            color = VhoqxethTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Your device IP is verified and mapped to:",
            color = VhoqxethTextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // User badge card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VhoqxethSurfaceElevated)
                .border(1.dp, VhoqxethBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(VhoqxethPrimaryViolet, VhoqxethCyanAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.displayName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = entry.displayName,
                        color = VhoqxethTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "@${entry.customUserId}",
                        color = VhoqxethCyanBright,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Login / Continue button
        Button(
            onClick = onContinue,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("login_continue_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VhoqxethPrimaryViolet,
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Continue to Chats",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Continue",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onSwitchAccount,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("switch_account_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = VhoqxethTextSecondary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, VhoqxethBorder)
        ) {
            Text(
                text = "Switch Account or Register New",
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun UnrecognizedStepContent(
    ip: String,
    onCreateAccount: () -> Unit,
    onSignInExisting: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VhoqxethSurface)
            .border(1.dp, VhoqxethBorder, RoundedCornerShape(16.dp))
            .padding(22.dp)
            .testTag("unrecognized_ip_card"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(VhoqxethPrimaryViolet.copy(alpha = 0.2f))
                .border(1.5.dp, VhoqxethPrimaryVioletBright, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "New Device",
                tint = VhoqxethCyanAccent,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Device IP Not Registered",
            color = VhoqxethTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "IP ($ip) is not associated with any account yet. Select an option below to proceed:",
            color = VhoqxethTextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Option 1: Create Account
        Button(
            onClick = onCreateAccount,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("create_account_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VhoqxethPrimaryViolet,
                contentColor = Color.White
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create Account",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Option 2: Link Existing Account / Login by Custom User ID
        OutlinedButton(
            onClick = onSignInExisting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("link_existing_account_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = VhoqxethCyanAccent
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, VhoqxethCyanAccent.copy(alpha = 0.6f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log In with Custom User ID",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RegisterFormContent(
    ip: String,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSubmit: (name: String, customUserId: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var customUserId by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val isNameValid = name.trim().length >= 2
    val isCustomIdValid = customUserId.trim().matches(Regex("^[a-zA-Z0-9_]{3,20}$"))
    val isFormValid = isNameValid && isCustomIdValid

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VhoqxethSurface)
            .border(1.dp, VhoqxethBorder, RoundedCornerShape(16.dp))
            .padding(22.dp)
            .testTag("registration_form_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                enabled = !isLoading,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = VhoqxethTextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create Account",
                color = VhoqxethTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Enter your profile details. Your device IP ($ip) will be registered to this identity in the database:",
            color = VhoqxethTextMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display Name Field
        Text(
            text = "NAME",
            color = VhoqxethTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("e.g. Alex Hunter", color = VhoqxethTextMuted) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Name",
                    tint = VhoqxethCyanAccent
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("name_input_field"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VhoqxethSurfaceElevated,
                unfocusedContainerColor = VhoqxethSurfaceElevated,
                focusedBorderColor = VhoqxethCyanAccent,
                unfocusedBorderColor = VhoqxethBorder,
                focusedTextColor = VhoqxethTextPrimary,
                unfocusedTextColor = VhoqxethTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Custom User ID Field
        Text(
            text = "CUSTOM USER ID",
            color = VhoqxethTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = customUserId,
            onValueChange = { input ->
                customUserId = input.filter { it.isLetterOrDigit() || it == '_' }
            },
            placeholder = { Text("e.g. cypher_fox", color = VhoqxethTextMuted) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AlternateEmail,
                    contentDescription = "Handle",
                    tint = VhoqxethPrimaryVioletBright
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (isFormValid && !isLoading) {
                        onSubmit(name, customUserId)
                    }
                }
            ),
            supportingText = {
                Text(
                    text = if (customUserId.isNotEmpty() && !isCustomIdValid) {
                        "3–20 characters, letters, numbers, or underscores"
                    } else {
                        "Your custom handle on the network"
                    },
                    color = if (customUserId.isNotEmpty() && !isCustomIdValid) Color(0xFFFF8A80) else VhoqxethTextMuted,
                    fontSize = 11.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_user_id_input_field"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VhoqxethSurfaceElevated,
                unfocusedContainerColor = VhoqxethSurfaceElevated,
                focusedBorderColor = VhoqxethPrimaryVioletBright,
                unfocusedBorderColor = VhoqxethBorder,
                focusedTextColor = VhoqxethTextPrimary,
                unfocusedTextColor = VhoqxethTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                keyboardController?.hide()
                onSubmit(name, customUserId)
            },
            enabled = isFormValid && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_registration_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VhoqxethPrimaryViolet,
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Register Account on IP",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LinkExistingContent(
    ip: String,
    isLoading: Boolean,
    onBack: () -> Unit,
    onLink: (customUserId: String) -> Unit
) {
    var customUserId by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isValid = customUserId.trim().isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VhoqxethSurface)
            .border(1.dp, VhoqxethBorder, RoundedCornerShape(16.dp))
            .padding(22.dp)
            .testTag("link_existing_card"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                enabled = !isLoading,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = VhoqxethTextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Log In with Custom User ID",
                color = VhoqxethTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Enter your existing Custom User ID to link this device IP ($ip) to your profile and log in:",
            color = VhoqxethTextMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = customUserId,
            onValueChange = { customUserId = it.trim() },
            placeholder = { Text("e.g. cypher_fox", color = VhoqxethTextMuted) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AlternateEmail,
                    contentDescription = "User ID",
                    tint = VhoqxethCyanAccent
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (isValid && !isLoading) onLink(customUserId)
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("existing_custom_id_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VhoqxethSurfaceElevated,
                unfocusedContainerColor = VhoqxethSurfaceElevated,
                focusedBorderColor = VhoqxethCyanAccent,
                unfocusedBorderColor = VhoqxethBorder,
                focusedTextColor = VhoqxethTextPrimary,
                unfocusedTextColor = VhoqxethTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                keyboardController?.hide()
                onLink(customUserId)
            },
            enabled = isValid && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_link_ip_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VhoqxethPrimaryViolet,
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Link",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Link Device IP & Log In",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
