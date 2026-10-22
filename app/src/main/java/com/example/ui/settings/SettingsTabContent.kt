package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSelfHealingEngine
import com.example.data.AuthRepository
import com.example.data.SessionManager
import com.example.model.UserProfile
import com.example.ui.chat.CloudflareWorkerSettingsDialog
import com.example.ui.theme.VhoqxethBorder
import com.example.ui.theme.VhoqxethBorderLightStrong
import com.example.ui.theme.VhoqxethCyanAccent
import com.example.ui.theme.VhoqxethDarkBg
import com.example.ui.theme.VhoqxethIndigo600
import com.example.ui.theme.VhoqxethOnlineGreen
import com.example.ui.theme.VhoqxethPrimaryViolet
import com.example.ui.theme.VhoqxethSlate900
import com.example.ui.theme.VhoqxethSurface
import com.example.ui.theme.VhoqxethSurfaceElevated
import com.example.ui.theme.VhoqxethTextMuted
import com.example.ui.theme.VhoqxethTextPrimary
import com.example.ui.theme.VhoqxethTextSecondary

@Composable
fun SettingsTabContent(
    userProfile: UserProfile?,
    databaseId: String,
    authRepository: AuthRepository,
    onProfileUpdated: (UserProfile) -> Unit,
    onSignOut: () -> Unit,
    onShowAboutApp: () -> Unit,
    onShowPrivacyPolicy: () -> Unit,
    onShowTerms: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showProfileEditor by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showCloudflareSettings by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val healingEngine = remember { AppSelfHealingEngine.getInstance(context) }
    val totalHealedCount by healingEngine.totalHealedCount.collectAsState()

    var notificationsEnabled by remember { mutableStateOf(true) }
    var previewLockEnabled by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. User Profile Hero Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, VhoqxethBorderLightStrong, RoundedCornerShape(20.dp))
                    .padding(20.dp)
                    .testTag("settings_profile_card")
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(VhoqxethIndigo600),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile?.displayName?.take(2)?.uppercase() ?: "VQ",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile?.displayName ?: "Mesh User",
                                color = VhoqxethSlate900,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "@${userProfile?.customUserId ?: "handle"}",
                                    color = VhoqxethIndigo600,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Immutable handle",
                                    tint = VhoqxethTextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            if (userProfile?.firstName?.isNotBlank() == true || userProfile?.lastName?.isNotBlank() == true) {
                                Text(
                                    text = "${userProfile.firstName} ${userProfile.lastName}".trim(),
                                    color = VhoqxethTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (userProfile?.description?.isNotBlank() == true) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = userProfile.description,
                                color = VhoqxethTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showProfileEditor = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VhoqxethIndigo600,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_profile_editor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Profile Details", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 2. Profile & Security Category
        item {
            SettingsCategoryCard(title = "PROFILE & SECURITY") {
                SettingsNavigationRow(
                    icon = Icons.Default.Person,
                    title = "Edit Profile Details",
                    subtitle = "Visible name, full name, birthday & bio",
                    onClick = { showProfileEditor = true },
                    testTag = "settings_item_edit_profile"
                )
                HorizontalDivider(color = VhoqxethBorder, thickness = 0.5.dp)
                SettingsNavigationRow(
                    icon = Icons.Default.VpnKey,
                    title = "Change Password",
                    subtitle = "Update your enclave account password",
                    onClick = { showChangePassword = true },
                    testTag = "settings_item_change_password"
                )
            }
        }

        // 3. Active Network IP & Connection Status
        item {
            SettingsCategoryCard(title = "NETWORK IP & DATABASE") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VhoqxethOnlineGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = VhoqxethOnlineGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Network IP",
                            color = VhoqxethTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = userProfile?.currentIp?.ifBlank { "Local Network" } ?: "Resolved",
                            color = VhoqxethCyanAccent,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(VhoqxethOnlineGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ONLINE",
                            color = VhoqxethOnlineGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = VhoqxethBorder, thickness = 0.5.dp)

                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "FIRESTORE CLUSTER",
                        color = VhoqxethTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = databaseId,
                        color = VhoqxethTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 4. Notifications & Privacy
        item {
            SettingsCategoryCard(title = "NOTIFICATIONS & PRIVACY") {
                SettingsSwitchRow(
                    icon = Icons.Default.Notifications,
                    title = "In-App Notifications",
                    subtitle = "Alert on new incoming mesh dispatches",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
                HorizontalDivider(color = VhoqxethBorder, thickness = 0.5.dp)
                SettingsSwitchRow(
                    icon = Icons.Default.Security,
                    title = "Message Preview Lock",
                    subtitle = "Require tap to reveal message text in threads",
                    checked = previewLockEnabled,
                    onCheckedChange = { previewLockEnabled = it }
                )
            }
        }

        // 5. Local Storage & Cache Cleaner
        item {
            SettingsCategoryCard(title = "STORAGE & DIAGNOSTICS") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onShowSnackbar("Local memory cache cleaned & refreshed")
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VhoqxethSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cached,
                            contentDescription = null,
                            tint = VhoqxethCyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clear Local Session Cache",
                            color = VhoqxethTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Clears transient in-memory buffers",
                            color = VhoqxethTextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Clean",
                        color = VhoqxethIndigo600,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = VhoqxethBorder, thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            healingEngine.triggerManualAudit(coroutineScope) { count ->
                                onShowSnackbar(if (count > 0) "AI Self-Healing: Repaired $count anomalies" else "AI Self-Healing: All subsystems nominal")
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VhoqxethOnlineGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Guardian",
                            tint = VhoqxethOnlineGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Autonomous AI Self-Healing",
                            color = VhoqxethTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (totalHealedCount > 0) "Continuous scan • $totalHealedCount auto-repaired" else "Continuous scan • All systems nominal",
                            color = VhoqxethOnlineGreen,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Audit",
                        color = VhoqxethCyanAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = VhoqxethBorder, thickness = 0.5.dp)

                SettingsNavigationRow(
                    icon = Icons.Default.CloudQueue,
                    title = "Cloudflare Edge Gatekeeper",
                    subtitle = "Backblaze B2 S3 pre-signed URL generator (${sessionManager.getCloudflareWorkerUrl()})",
                    onClick = { showCloudflareSettings = true },
                    testTag = "settings_item_cloudflare_worker"
                )
            }
        }

        // 6. Protocol Charter & Legal
        item {
            SettingsCategoryCard(title = "PROTOCOL CHARTER") {
                SettingsNavigationRow(
                    icon = Icons.Default.Info,
                    title = "About the App",
                    subtitle = "Vhoqxeth mesh protocol & architecture",
                    onClick = onShowAboutApp,
                    testTag = "settings_about_app"
                )
                HorizontalDivider(color = VhoqxethBorder, thickness = 0.5.dp)
                SettingsNavigationRow(
                    icon = Icons.Default.Policy,
                    title = "Privacy Policy",
                    subtitle = "Cryptographic data governance charter",
                    onClick = onShowPrivacyPolicy,
                    testTag = "settings_privacy_policy"
                )
                HorizontalDivider(color = VhoqxethBorder, thickness = 0.5.dp)
                SettingsNavigationRow(
                    icon = Icons.Default.Gavel,
                    title = "Terms & Conditions",
                    subtitle = "Decentralized mesh terms of service",
                    onClick = onShowTerms,
                    testTag = "settings_terms_conditions"
                )
            }
        }

        // 7. Sign Out Action
        item {
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign Out",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of Enclave", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal dialogs
    if (showProfileEditor && userProfile != null) {
        ProfileEditorDialog(
            currentUser = userProfile,
            authRepository = authRepository,
            onDismiss = { showProfileEditor = false },
            onProfileUpdated = { updated ->
                onProfileUpdated(updated)
                onShowSnackbar("Profile details updated successfully")
            }
        )
    }

    if (showChangePassword && userProfile != null) {
        ChangePasswordDialog(
            currentUser = userProfile,
            authRepository = authRepository,
            onDismiss = { showChangePassword = false },
            onSuccess = {
                onShowSnackbar("Password updated successfully")
            }
        )
    }

    if (showCloudflareSettings) {
        CloudflareWorkerSettingsDialog(
            currentUrl = sessionManager.getCloudflareWorkerUrl(),
            onSaveUrl = { newUrl ->
                sessionManager.setCloudflareWorkerUrl(newUrl)
                onShowSnackbar("Cloudflare Worker URL updated")
            },
            onDismiss = { showCloudflareSettings = false }
        )
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            color = VhoqxethTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(VhoqxethSurfaceElevated)
                .border(1.dp, VhoqxethBorder, RoundedCornerShape(16.dp))
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(VhoqxethSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VhoqxethCyanAccent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = VhoqxethTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = VhoqxethTextMuted,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = VhoqxethTextMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(VhoqxethSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VhoqxethCyanAccent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = VhoqxethTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = VhoqxethTextMuted,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VhoqxethIndigo600,
                uncheckedThumbColor = VhoqxethTextMuted,
                uncheckedTrackColor = VhoqxethSurface
            )
        )
    }
}
