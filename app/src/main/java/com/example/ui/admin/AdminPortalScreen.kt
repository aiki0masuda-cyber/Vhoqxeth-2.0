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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminRepository
import com.example.data.AppSelfHealingEngine
import com.example.data.HealingLogEntry
import com.example.model.RegisteredIpEntry
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
import kotlinx.coroutines.launch

val AdminAmber = Color(0xFFFFB300)
val AdminCrimson = Color(0xFFFF5252)
val AdminSurface = Color(0xFF14121E)
val AdminCardBg = Color(0xFF1B1829)
val AdminBorder = Color(0xFF2E2A44)

@Composable
fun AdminPortalScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val adminRepository = remember { AdminRepository(context) }
    val healingEngine = remember { AppSelfHealingEngine.getInstance(context) }

    val totalHealedCount by healingEngine.totalHealedCount.collectAsState()
    val isHealingScanning by healingEngine.isScanning.collectAsState()
    val engineStatus by healingEngine.engineStatus.collectAsState()
    val healingLogs by healingEngine.healingLogs.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Data lists
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var registeredIps by remember { mutableStateOf<List<RegisteredIpEntry>>(emptyList()) }

    // Quarantined user account state
    var quarantinedUser by remember { mutableStateOf<UserProfile?>(null) }

    // Dialog states
    var showAddUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserProfile?>(null) }
    var deletingUser by remember { mutableStateOf<UserProfile?>(null) }
    var showAddIpDialog by remember { mutableStateOf(false) }

    fun refreshData() {
        isLoading = true
        statusMessage = null
        coroutineScope.launch {
            val usersRes = adminRepository.getAllUsers()
            val ipsRes = adminRepository.getAllRegisteredIps()

            users = usersRes.getOrDefault(emptyList())
            registeredIps = ipsRes.getOrDefault(emptyList())
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VhoqxethDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("admin_portal_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AdminSurface)
                    .border(1.dp, VhoqxethBorder)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AdminAmber.copy(alpha = 0.2f))
                        .border(1.5.dp, AdminAmber, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "Admin Root",
                        tint = AdminAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MASTER ENCLAVE ADMIN",
                        color = AdminAmber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "FULL DATABASE ACCESS • ROOT PRIVILEGES",
                        color = VhoqxethTextMuted,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = { refreshData() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = VhoqxethCyanAccent
                    )
                }

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier
                        .height(34.dp)
                        .padding(start = 6.dp)
                        .testTag("admin_exit_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminCrimson),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.6f))
                ) {
                    Text("Exit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AdminSurface,
                contentColor = VhoqxethTextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AdminAmber
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Users (${users.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) AdminAmber else VhoqxethTextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.testTag("admin_tab_users")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "IP Registry (${registeredIps.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) AdminAmber else VhoqxethTextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.testTag("admin_tab_ips")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "AI Guardian ($totalHealedCount)",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) AdminAmber else VhoqxethTextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.testTag("admin_tab_ai_health")
                )
            }

            // Status / Error message
            statusMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2C2210))
                        .border(1.dp, AdminAmber, RoundedCornerShape(0.dp))
                        .padding(10.dp)
                ) {
                    Text(text = msg, color = AdminAmber, fontSize = 12.sp)
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AdminAmber,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> UsersTabContent(
                    users = users,
                    onAddUser = { showAddUserDialog = true },
                    onQuarantineUser = { user -> quarantinedUser = user },
                    onEditUser = { user -> editingUser = user },
                    onDeleteUser = { user -> deletingUser = user }
                )
                1 -> IpRegistryTabContent(
                    ips = registeredIps,
                    onAddIp = { showAddIpDialog = true },
                    onDeleteIp = { ipEntry ->
                        coroutineScope.launch {
                            adminRepository.deleteRegisteredIp(ipEntry.ip, ipEntry.customUserId)
                            refreshData()
                        }
                    }
                )
                2 -> AiHealthTabContent(
                    totalHealedCount = totalHealedCount,
                    isScanning = isHealingScanning,
                    engineStatus = engineStatus,
                    healingLogs = healingLogs,
                    onTriggerScan = {
                        healingEngine.triggerManualAudit(coroutineScope) { count ->
                            statusMessage = if (count > 0) "AI Health Audit complete: $count anomalies auto-repaired" else "AI Health Audit complete: All systems nominal"
                        }
                    }
                )
            }
        }
    }

    // Quarantined Account Enclave Sheet
    quarantinedUser?.let { target ->
        QuarantinedAccountSheet(
            targetUser = target,
            onDismiss = { quarantinedUser = null },
            onUserModified = { refreshData() }
        )
    }

    // Add User Dialog
    if (showAddUserDialog) {
        AddUserDialog(
            onDismiss = { showAddUserDialog = false },
            onConfirm = { name, customId, ip, role, grade, section, subject, department, employeeId, designation ->
                coroutineScope.launch {
                    val res = adminRepository.createUser(
                        displayName = name,
                        customUserId = customId,
                        initialIp = ip,
                        role = role,
                        grade = grade,
                        section = section,
                        subject = subject,
                        department = department,
                        employeeId = employeeId,
                        designation = designation
                    )
                    if (res.isSuccess) {
                        val roleTag = if (role == UserProfile.ROLE_TEACHER) "Teacher" else "Student"
                        statusMessage = "Created $roleTag account @$customId"
                        showAddUserDialog = false
                        refreshData()
                    } else {
                        statusMessage = "Error: ${res.exceptionOrNull()?.message}"
                    }
                }
            }
        )
    }

    // Edit User Dialog
    editingUser?.let { user ->
        EditUserDialog(
            user = user,
            onDismiss = { editingUser = null },
            onSave = { updated ->
                coroutineScope.launch {
                    val res = adminRepository.updateUser(user.customUserId, updated)
                    if (res.isSuccess) {
                        statusMessage = "Updated user @${updated.customUserId}"
                        editingUser = null
                        refreshData()
                    } else {
                        statusMessage = "Error: ${res.exceptionOrNull()?.message}"
                    }
                }
            }
        )
    }

    // Delete User Confirmation Dialog
    deletingUser?.let { user ->
        AlertDialog(
            onDismissRequest = { deletingUser = null },
            containerColor = AdminSurface,
            title = {
                Text("Confirm User Deletion", color = AdminCrimson, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to permanently delete user @${user.customUserId} (${user.displayName})? This will also purge and unlink all ${user.associatedIps.size} associated device IPs from the database.",
                    color = VhoqxethTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            adminRepository.deleteUser(user.customUserId, user.associatedIps)
                            deletingUser = null
                            statusMessage = "Deleted user @${user.customUserId} and unlinked IPs"
                            refreshData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminCrimson)
                ) {
                    Text("Delete User & IPs", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingUser = null }) {
                    Text("Cancel", color = VhoqxethTextSecondary)
                }
            }
        )
    }

    // Add IP Dialog
    if (showAddIpDialog) {
        AddIpDialog(
            onDismiss = { showAddIpDialog = false },
            onConfirm = { ip, handle, name ->
                coroutineScope.launch {
                    adminRepository.addRegisteredIp(ip, handle, name)
                    showAddIpDialog = false
                    refreshData()
                }
            }
        )
    }
}

@Composable
private fun UsersTabContent(
    users: List<UserProfile>,
    onAddUser: () -> Unit,
    onQuarantineUser: (UserProfile) -> Unit,
    onEditUser: (UserProfile) -> Unit,
    onDeleteUser: (UserProfile) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = users.filter {
        it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.customUserId.contains(searchQuery, ignoreCase = true) ||
                it.associatedIps.any { ip -> ip.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search users or handles...", color = VhoqxethTextMuted) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = VhoqxethCyanAccent)
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("admin_user_search"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AdminCardBg,
                    unfocusedContainerColor = AdminCardBg,
                    focusedBorderColor = VhoqxethCyanAccent,
                    unfocusedBorderColor = VhoqxethBorder,
                    focusedTextColor = VhoqxethTextPrimary,
                    unfocusedTextColor = VhoqxethTextPrimary
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onAddUser,
                modifier = Modifier
                    .height(50.dp)
                    .testTag("admin_add_user_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminAmber)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add User", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No users registered in database." else "No matching users found.",
                    color = VhoqxethTextMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers, key = { it.customUserId }) { user ->
                    UserAdminCard(
                        user = user,
                        onQuarantine = { onQuarantineUser(user) },
                        onEdit = { onEditUser(user) },
                        onDelete = { onDeleteUser(user) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserAdminCard(
    user: UserProfile,
    onQuarantine: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onQuarantine() }
            .testTag("admin_user_card_${user.customUserId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AdminCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, VhoqxethBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(VhoqxethPrimaryViolet, VhoqxethCyanAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.displayName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName,
                        color = VhoqxethTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${user.customUserId}",
                        color = VhoqxethCyanBright,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = onQuarantine,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminAmber.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AdminAmber),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("quarantine_btn_${user.customUserId}")
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = AdminAmber, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("QUARANTINE", color = AdminAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp).testTag("edit_user_${user.customUserId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = AdminAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("delete_user_${user.customUserId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AdminCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = VhoqxethBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "LINKED IPS:",
                    color = VhoqxethTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (user.associatedIps.isEmpty()) {
                    Text("None", color = VhoqxethTextMuted, fontSize = 11.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(user.associatedIps) { ip ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AdminSurface)
                                    .border(0.5.dp, VhoqxethCyanAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = ip,
                                    color = VhoqxethCyanAccent,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
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
private fun AiHealthTabContent(
    totalHealedCount: Int,
    isScanning: Boolean,
    engineStatus: String,
    healingLogs: List<HealingLogEntry>,
    onTriggerScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // AI Guardian Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AdminCardBg),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AdminAmber.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VhoqxethOnlineGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Guardian",
                                tint = VhoqxethOnlineGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AUTONOMOUS AI SELF-HEALING",
                                color = AdminAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = engineStatus,
                                color = VhoqxethOnlineGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(VhoqxethOnlineGreen)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AdminSurface)
                            .border(0.5.dp, VhoqxethBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("AUTO-REPAIRED", color = VhoqxethTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$totalHealedCount Issues", color = AdminAmber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AdminSurface)
                            .border(0.5.dp, VhoqxethBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("SCAN INTERVAL", color = VhoqxethTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Every 35s", color = VhoqxethCyanAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AdminSurface)
                            .border(0.5.dp, VhoqxethBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("SUBSYSTEMS", color = VhoqxethTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("4 Monitored", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onTriggerScan,
                    enabled = !isScanning,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("admin_run_ai_scan_button")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Executing Deep Self-Healing Audit...", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trigger Immediate AI Health Audit", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CONTINUOUS DIAGNOSTIC AUDIT LOGS (${healingLogs.size})",
            color = AdminAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (healingLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("All systems nominal. No errors detected.", color = VhoqxethTextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(healingLogs.reversed(), key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AdminCardBg),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, VhoqxethBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (log.isAutoHealed) VhoqxethOnlineGreen else VhoqxethCyanAccent)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.subsystem,
                                        color = AdminAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = log.timestamp,
                                        color = VhoqxethTextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.description,
                                    color = VhoqxethTextPrimary,
                                    fontSize = 12.sp
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
private fun IpRegistryTabContent(
    ips: List<RegisteredIpEntry>,
    onAddIp: () -> Unit,
    onDeleteIp: (RegisteredIpEntry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GLOBAL IP INDEX TABLE",
                color = AdminAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Button(
                onClick = onAddIp,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminAmber),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Map IP", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (ips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No device IPs currently mapped.", color = VhoqxethTextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ips, key = { it.ip }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = AdminCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VhoqxethBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = VhoqxethCyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.ip,
                                    color = VhoqxethTextPrimary,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${entry.displayName} (@${entry.customUserId})",
                                    color = VhoqxethTextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            IconButton(
                                onClick = { onDeleteIp(entry) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Unlink IP",
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
private fun AddUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        customId: String,
        ip: String,
        role: String,
        grade: String,
        section: String,
        subject: String,
        department: String,
        employeeId: String,
        designation: String
    ) -> Unit
) {
    var selectedRole by remember { mutableStateOf(UserProfile.ROLE_STUDENT) }
    var name by remember { mutableStateOf("") }
    var customId by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }

    // Student fields
    var grade by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    // Teacher fields
    var department by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }

    val isTeacher = selectedRole == UserProfile.ROLE_TEACHER

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        title = {
            Text(
                text = "Add Institutional User",
                color = AdminAmber,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SELECT INSTITUTIONAL ROLE",
                    color = VhoqxethTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // Role Selector: Student vs Teacher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isStudentSel = selectedRole == UserProfile.ROLE_STUDENT
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isStudentSel) AdminAmber.copy(alpha = 0.2f) else AdminCardBg)
                            .border(
                                width = if (isStudentSel) 1.5.dp else 1.dp,
                                color = if (isStudentSel) AdminAmber else AdminBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedRole = UserProfile.ROLE_STUDENT }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Student",
                                tint = if (isStudentSel) AdminAmber else VhoqxethTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Student",
                                color = if (isStudentSel) AdminAmber else VhoqxethTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val isTeacherSel = selectedRole == UserProfile.ROLE_TEACHER
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isTeacherSel) VhoqxethCyanAccent.copy(alpha = 0.2f) else AdminCardBg)
                            .border(
                                width = if (isTeacherSel) 1.5.dp else 1.dp,
                                color = if (isTeacherSel) VhoqxethCyanAccent else AdminBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedRole = UserProfile.ROLE_TEACHER }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Teacher",
                                tint = if (isTeacherSel) VhoqxethCyanAccent else VhoqxethTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Teacher",
                                color = if (isTeacherSel) VhoqxethCyanAccent else VhoqxethTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = AdminBorder, thickness = 0.5.dp)

                // Role-specific fields
                if (isTeacher) {
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department / Subject Area") },
                        placeholder = { Text("e.g. Mathematics, Physics") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = VhoqxethTextPrimary,
                            unfocusedTextColor = VhoqxethTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = designation,
                        onValueChange = { designation = it },
                        label = { Text("Designation") },
                        placeholder = { Text("e.g. Senior PGT, Lecturer") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = VhoqxethTextPrimary,
                            unfocusedTextColor = VhoqxethTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = employeeId,
                        onValueChange = { employeeId = it },
                        label = { Text("Employee ID") },
                        placeholder = { Text("e.g. DAV-FAC-204") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = VhoqxethTextPrimary,
                            unfocusedTextColor = VhoqxethTextPrimary
                        )
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = grade,
                            onValueChange = { grade = it },
                            label = { Text("Grade / Class") },
                            placeholder = { Text("10, 11, 12") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = VhoqxethTextPrimary,
                                unfocusedTextColor = VhoqxethTextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = section,
                            onValueChange = { section = it },
                            label = { Text("Section") },
                            placeholder = { Text("A, B, C") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = VhoqxethTextPrimary,
                                unfocusedTextColor = VhoqxethTextPrimary
                            )
                        )
                    }
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject Specialization (optional)") },
                        placeholder = { Text("e.g. Science, Commerce, Arts") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = VhoqxethTextPrimary,
                            unfocusedTextColor = VhoqxethTextPrimary
                        )
                    )
                }

                HorizontalDivider(color = AdminBorder, thickness = 0.5.dp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    placeholder = { Text(if (isTeacher) "e.g. Dr. Rajesh Sharma" else "e.g. Rahul Verma") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VhoqxethTextPrimary,
                        unfocusedTextColor = VhoqxethTextPrimary
                    )
                )
                OutlinedTextField(
                    value = customId,
                    onValueChange = { customId = it.filter { c -> c.isLetterOrDigit() || c == '_' } },
                    label = { Text("Custom User ID (@handle)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VhoqxethTextPrimary,
                        unfocusedTextColor = VhoqxethTextPrimary
                    )
                )
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it.trim() },
                    label = { Text("Initial Device IP (optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VhoqxethTextPrimary,
                        unfocusedTextColor = VhoqxethTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        customId,
                        ip,
                        selectedRole,
                        grade,
                        section,
                        subject,
                        department,
                        employeeId,
                        designation
                    )
                },
                enabled = name.isNotBlank() && customId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTeacher) VhoqxethCyanAccent else AdminAmber
                )
            ) {
                Text(
                    text = if (isTeacher) "Create Faculty User" else "Create Student User",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = VhoqxethTextSecondary)
            }
        }
    )
}

@Composable
private fun EditUserDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(user.displayName) }
    var customId by remember { mutableStateOf(user.customUserId) }
    var newIpToAdd by remember { mutableStateOf("") }
    var ipsList by remember { mutableStateOf(user.associatedIps.toMutableList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        title = { Text("Edit User @${user.customUserId}", color = AdminAmber, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VhoqxethTextPrimary,
                        unfocusedTextColor = VhoqxethTextPrimary
                    )
                )
                OutlinedTextField(
                    value = customId,
                    onValueChange = { customId = it.filter { c -> c.isLetterOrDigit() || c == '_' } },
                    label = { Text("Custom User ID") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VhoqxethTextPrimary,
                        unfocusedTextColor = VhoqxethTextPrimary
                    )
                )

                Text(
                    text = "ASSOCIATED DEVICE IPS",
                    color = VhoqxethTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // Chips for associated IPs with remove button
                ipsList.forEach { ip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(AdminCardBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ip,
                            color = VhoqxethCyanAccent,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                ipsList = ipsList.filterNot { it == ip }.toMutableList()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = AdminCrimson, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newIpToAdd,
                        onValueChange = { newIpToAdd = it.trim() },
                        placeholder = { Text("Add IP address...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = VhoqxethTextPrimary,
                            unfocusedTextColor = VhoqxethTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newIpToAdd.isNotBlank() && !ipsList.contains(newIpToAdd)) {
                                ipsList = (ipsList + newIpToAdd).toMutableList()
                                newIpToAdd = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AdminAmber)
                    ) {
                        Text("Add", color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        user.copy(
                            displayName = name,
                            customUserId = customId,
                            associatedIps = ipsList,
                            currentIp = ipsList.firstOrNull() ?: ""
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminAmber)
            ) {
                Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = VhoqxethTextSecondary)
            }
        }
    )
}

@Composable
private fun AddIpDialog(
    onDismiss: () -> Unit,
    onConfirm: (ip: String, handle: String, name: String) -> Unit
) {
    var ip by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        title = { Text("Map Device IP to User", color = AdminAmber, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it.trim() },
                    label = { Text("IP Address (e.g. 198.51.100.2)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VhoqxethTextPrimary,
                        unfocusedTextColor = VhoqxethTextPrimary
                    )
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it.trim().removePrefix("@") },
                    label = { Text("User Handle (@handle)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VhoqxethTextPrimary,
                        unfocusedTextColor = VhoqxethTextPrimary
                    )
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VhoqxethTextPrimary,
                        unfocusedTextColor = VhoqxethTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(ip, handle, name) },
                enabled = ip.isNotBlank() && handle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AdminAmber)
            ) {
                Text("Save Route", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = VhoqxethTextSecondary)
            }
        }
    )
}
