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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
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
import kotlinx.coroutines.launch

// Light Minimalist Color Palette
val AdminBg = Color(0xFFF8FAFC)          // Soft slate canvas
val AdminSurface = Color(0xFFF8FAFC)      // Slate canvas alias
val AdminCard = Color(0xFFFFFFFF)        // Crisp pure white cards
val AdminCardBg = Color(0xFFFFFFFF)      // Crisp card background alias
val AdminBorder = Color(0xFFE2E8F0)      // Slate 200 border
val AdminTextPrimary = Color(0xFF0F172A) // Slate 900
val AdminTextSecondary = Color(0xFF475569) // Slate 600
val AdminTextMuted = Color(0xFF94A3B8)   // Slate 400
val AdminBrand = Color(0xFF2563EB)       // Royal Blue
val AdminBrandLight = Color(0xFFEFF6FF)  // Blue 50
val AdminDanger = Color(0xFFDC2626)      // Red 600
val AdminCrimson = Color(0xFFDC2626)     // Danger red alias
val AdminDangerLight = Color(0xFFFEF2F2) // Red 50
val AdminDangerBorder = Color(0xFFFECACA)// Red 200
val AdminSuccess = Color(0xFF16A34A)     // Emerald 600
val AdminSuccessLight = Color(0xFFF0FDF4)// Emerald 50
val AdminAmber = Color(0xFFD97706)       // Amber 600
val AdminAmberLight = Color(0xFFFFFBEB)  // Amber 50

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
    var isResettingDb by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isStatusSuccess by remember { mutableStateOf(true) }

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
    var showFactoryResetDialog by remember { mutableStateOf(false) }

    fun refreshData() {
        isLoading = true
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
            .background(AdminBg)
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
                    .background(AdminCard)
                    .border(1.dp, AdminBorder)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AdminBrandLight)
                        .border(1.dp, AdminBrand.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "Admin Root",
                        tint = AdminBrand,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Campus Administration",
                        color = AdminTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Database Management Console • Root Privileges",
                        color = AdminTextSecondary,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = { refreshData() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = AdminBrand
                    )
                }

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier
                        .height(34.dp)
                        .padding(start = 6.dp)
                        .testTag("admin_exit_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminDanger),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AdminDangerBorder)
                ) {
                    Text("Exit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Minimalist Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AdminCard,
                contentColor = AdminBrand,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AdminBrand
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Users (${users.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) AdminBrand else AdminTextSecondary,
                            fontSize = 12.sp
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
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) AdminBrand else AdminTextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("admin_tab_ips")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "Health ($totalHealedCount)",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 2) AdminBrand else AdminTextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("admin_tab_ai_health")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            "Database Reset",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 3) AdminDanger else AdminTextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("admin_tab_database_reset")
                )
            }

            // Status Banner
            statusMessage?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isStatusSuccess) AdminSuccessLight else AdminDangerLight)
                        .border(1.dp, if (isStatusSuccess) Color(0xFFBBF7D0) else AdminDangerBorder)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isStatusSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isStatusSuccess) AdminSuccess else AdminDanger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg,
                        color = if (isStatusSuccess) AdminSuccess else AdminDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { statusMessage = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = if (isStatusSuccess) AdminSuccess else AdminDanger,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AdminBrand,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(26.dp)
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
                            isStatusSuccess = true
                            statusMessage = "Unlinked IP ${ipEntry.ip}"
                            refreshData()
                        }
                    }
                )
                2 -> SystemHealthTabContent(
                    totalHealedCount = totalHealedCount,
                    isScanning = isHealingScanning,
                    engineStatus = engineStatus,
                    healingLogs = healingLogs,
                    onTriggerScan = {
                        healingEngine.triggerManualAudit(coroutineScope) { count ->
                            isStatusSuccess = true
                            statusMessage = if (count > 0) "Health Audit complete: $count anomalies auto-repaired" else "Health Audit complete: All systems nominal"
                        }
                    }
                )
                3 -> DatabaseResetTabContent(
                    usersCount = users.size,
                    ipsCount = registeredIps.size,
                    isResetting = isResettingDb,
                    onTriggerReset = { showFactoryResetDialog = true }
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
                        isStatusSuccess = true
                        statusMessage = "Created $roleTag account @$customId"
                        showAddUserDialog = false
                        refreshData()
                    } else {
                        isStatusSuccess = false
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
                        isStatusSuccess = true
                        statusMessage = "Updated user @${updated.customUserId}"
                        editingUser = null
                        refreshData()
                    } else {
                        isStatusSuccess = false
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
            containerColor = AdminCard,
            title = {
                Text("Confirm User Deletion", color = AdminDanger, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to permanently delete user @${user.customUserId} (${user.displayName})? This will also purge and unlink all ${user.associatedIps.size} associated device IPs.",
                    color = AdminTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            adminRepository.deleteUser(user.customUserId, user.associatedIps)
                            deletingUser = null
                            isStatusSuccess = true
                            statusMessage = "Deleted user @${user.customUserId}"
                            refreshData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDanger)
                ) {
                    Text("Delete User", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingUser = null }) {
                    Text("Cancel", color = AdminTextSecondary)
                }
            }
        )
    }

    // Factory Reset Confirmation Dialog
    if (showFactoryResetDialog) {
        AlertDialog(
            onDismissRequest = { if (!isResettingDb) showFactoryResetDialog = false },
            containerColor = AdminCard,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AdminDangerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = AdminDanger,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Confirm Factory Database Reset",
                    color = AdminDanger,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This action will completely wipe all:",
                        color = AdminTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text("• All messages across all chat threads", color = AdminTextSecondary, fontSize = 12.sp)
                    Text("• All custom conversation channels", color = AdminTextSecondary, fontSize = 12.sp)
                    Text("• All user profiles and registered device IPs", color = AdminTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The system will be restored to clean factory defaults with the default Administrator account and General Campus channel.",
                        color = AdminTextPrimary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isResettingDb = true
                        coroutineScope.launch {
                            try {
                                val result = adminRepository.factoryResetDatabase()
                                if (result.isSuccess) {
                                    isStatusSuccess = true
                                    statusMessage = result.getOrNull() ?: "Database reset complete."
                                    showFactoryResetDialog = false
                                    refreshData()
                                } else {
                                    isStatusSuccess = false
                                    statusMessage = "Reset failed: ${result.exceptionOrNull()?.message}"
                                }
                            } finally {
                                isResettingDb = false
                            }
                        }
                    },
                    enabled = !isResettingDb,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDanger),
                    modifier = Modifier.testTag("confirm_factory_reset_button")
                ) {
                    if (isResettingDb) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resetting...", color = Color.White)
                    } else {
                        Text("Confirm Factory Reset", color = Color.White)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showFactoryResetDialog = false },
                    enabled = !isResettingDb
                ) {
                    Text("Cancel", color = AdminTextSecondary)
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
                    isStatusSuccess = true
                    statusMessage = "Registered IP $ip"
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
                placeholder = { Text("Search users, handles, or IPs...", color = AdminTextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = AdminBrand)
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("admin_user_search"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AdminCard,
                    unfocusedContainerColor = AdminCard,
                    focusedBorderColor = AdminBrand,
                    unfocusedBorderColor = AdminBorder,
                    focusedTextColor = AdminTextPrimary,
                    unfocusedTextColor = AdminTextPrimary
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onAddUser,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("admin_add_user_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminBrand)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add User", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                    color = AdminTextMuted,
                    fontSize = 13.sp
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
        colors = CardDefaults.cardColors(containerColor = AdminCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, AdminBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (user.isTeacher) Color(0xFFE0E7FF) else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.displayName.take(1).uppercase(),
                        color = if (user.isTeacher) AdminBrand else AdminTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.displayName,
                            color = AdminTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (user.isTeacher) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (user.isTeacher) "FACULTY" else "STUDENT",
                                color = if (user.isTeacher) AdminBrand else AdminTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "@${user.customUserId}",
                        color = AdminTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                OutlinedButton(
                    onClick = onQuarantine,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminBrand),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AdminBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("quarantine_btn_${user.customUserId}")
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = AdminBrand, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("INSPECT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp).testTag("edit_user_${user.customUserId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = AdminTextSecondary,
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
                        tint = AdminDanger,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AdminBg)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = AdminTextMuted, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (user.associatedIps.isEmpty()) "No device IPs registered" else "IPs: ${user.associatedIps.joinToString(", ")}",
                    color = AdminTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
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
                text = "REGISTERED NETWORK IPS (${ips.size})",
                color = AdminTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Button(
                onClick = onAddIp,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminBrand),
                modifier = Modifier.height(36.dp).testTag("admin_add_ip_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add IP", tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Register IP", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (ips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No device IPs currently mapped.", color = AdminTextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ips, key = { it.ip }) { ipEntry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = AdminCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AdminBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(AdminBrandLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = AdminBrand, modifier = Modifier.size(16.dp))
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ipEntry.ip,
                                    color = AdminTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Bound to @${ipEntry.customUserId} (${ipEntry.displayName})",
                                    color = AdminTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = { onDeleteIp(ipEntry) },
                                modifier = Modifier.size(32.dp).testTag("delete_ip_${ipEntry.ip}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Unlink IP",
                                    tint = AdminDanger,
                                    modifier = Modifier.size(16.dp)
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
private fun SystemHealthTabContent(
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AdminCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, AdminBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AdminSuccessLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = AdminSuccess, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Continuous Self-Healing Guardian", color = AdminTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(engineStatus, color = AdminSuccess, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AdminBg)
                            .border(1.dp, AdminBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("REPAIRS COMPLETED", color = AdminTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$totalHealedCount Fixed", color = AdminBrand, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AdminBg)
                            .border(1.dp, AdminBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("AUDIT CADENCE", color = AdminTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Active 35s", color = AdminTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onTriggerScan,
                    enabled = !isScanning,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminBrand),
                    modifier = Modifier.fillMaxWidth().height(40.dp).testTag("admin_run_ai_scan_button")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Running Diagnostic Audit...", color = Color.White, fontSize = 12.sp)
                    } else {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger Diagnostic Scan", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "SYSTEM AUDIT LOGS (${healingLogs.size})",
            color = AdminTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (healingLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("All systems nominal. No errors reported.", color = AdminTextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(healingLogs.reversed(), key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AdminCard),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AdminBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (log.isAutoHealed) AdminSuccess else AdminBrand)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = log.subsystem, color = AdminBrand, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = log.timestamp, color = AdminTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                Text(text = log.description, color = AdminTextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DatabaseResetTabContent(
    usersCount: Int,
    ipsCount: Int,
    isResetting: Boolean,
    onTriggerReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Reset Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = AdminCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, AdminDangerBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AdminDangerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Reset Database",
                            tint = AdminDanger,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Complete Factory Database Reset",
                            color = AdminDanger,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Purge all records and restore default clean state",
                            color = AdminTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Executing a factory reset will permanently remove:",
                    color = AdminTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(AdminDanger))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("All chat messages and voice notes across all channels", color = AdminTextSecondary, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(AdminDanger))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("All custom study circles and discussion groups", color = AdminTextSecondary, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(AdminDanger))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("All custom user profiles and registered device IP links", color = AdminTextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AdminAmberLight)
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = AdminAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "The system will auto-seed a fresh default General Campus channel and standard Administrator profile.",
                            color = AdminAmber,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onTriggerReset,
                    enabled = !isResetting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDanger),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("admin_factory_reset_button")
                ) {
                    if (isResetting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wiping and Resetting Database...", color = Color.White, fontSize = 13.sp)
                    } else {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Factory Reset Database", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Database Statistics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = AdminCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, AdminBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Current Database Footprint",
                    color = AdminTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AdminBg)
                            .border(1.dp, AdminBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("REGISTERED USERS", color = AdminTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$usersCount Profiles", color = AdminBrand, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AdminBg)
                            .border(1.dp, AdminBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("DEVICE IP MAPPINGS", color = AdminTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$ipsCount IPs", color = AdminTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
    var name by remember { mutableStateOf("") }
    var customId by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserProfile.ROLE_STUDENT) }
    var grade by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }

    val isTeacher = selectedRole == UserProfile.ROLE_TEACHER

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminCard,
        title = {
            Text(
                text = "Register Campus User",
                color = AdminTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Role Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isStudentSel = selectedRole == UserProfile.ROLE_STUDENT
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isStudentSel) AdminBrandLight else AdminBg)
                            .border(1.dp, if (isStudentSel) AdminBrand else AdminBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedRole = UserProfile.ROLE_STUDENT }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Student",
                            color = if (isStudentSel) AdminBrand else AdminTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    val isTeacherSel = selectedRole == UserProfile.ROLE_TEACHER
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isTeacherSel) AdminBrandLight else AdminBg)
                            .border(1.dp, if (isTeacherSel) AdminBrand else AdminBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedRole = UserProfile.ROLE_TEACHER }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Teacher / Faculty",
                            color = if (isTeacherSel) AdminBrand else AdminTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    placeholder = { Text(if (isTeacher) "e.g. Dr. Rajesh Sharma" else "e.g. Rahul Verma") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdminTextPrimary,
                        unfocusedTextColor = AdminTextPrimary
                    )
                )

                OutlinedTextField(
                    value = customId,
                    onValueChange = { customId = it.filter { c -> c.isLetterOrDigit() || c == '_' } },
                    label = { Text("Custom User ID (@handle)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdminTextPrimary,
                        unfocusedTextColor = AdminTextPrimary
                    )
                )

                if (isTeacher) {
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department") },
                        placeholder = { Text("e.g. Mathematics") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AdminTextPrimary,
                            unfocusedTextColor = AdminTextPrimary
                        )
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = grade,
                            onValueChange = { grade = it },
                            label = { Text("Grade") },
                            placeholder = { Text("10, 11, 12") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AdminTextPrimary,
                                unfocusedTextColor = AdminTextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = section,
                            onValueChange = { section = it },
                            label = { Text("Section") },
                            placeholder = { Text("A, B") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AdminTextPrimary,
                                unfocusedTextColor = AdminTextPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it.trim() },
                    label = { Text("Initial Device IP (optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdminTextPrimary,
                        unfocusedTextColor = AdminTextPrimary
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
                colors = ButtonDefaults.buttonColors(containerColor = AdminBrand)
            ) {
                Text("Register User", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = AdminTextSecondary)
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
    var displayName by remember { mutableStateOf(user.displayName) }
    var grade by remember { mutableStateOf(user.grade) }
    var section by remember { mutableStateOf(user.section) }
    var department by remember { mutableStateOf(user.department) }
    var designation by remember { mutableStateOf(user.designation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminCard,
        title = {
            Text("Edit User @${user.customUserId}", color = AdminTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdminTextPrimary,
                        unfocusedTextColor = AdminTextPrimary
                    )
                )

                if (user.isTeacher) {
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AdminTextPrimary,
                            unfocusedTextColor = AdminTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = designation,
                        onValueChange = { designation = it },
                        label = { Text("Designation") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AdminTextPrimary,
                            unfocusedTextColor = AdminTextPrimary
                        )
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = grade,
                            onValueChange = { grade = it },
                            label = { Text("Grade") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AdminTextPrimary,
                                unfocusedTextColor = AdminTextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = section,
                            onValueChange = { section = it },
                            label = { Text("Section") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AdminTextPrimary,
                                unfocusedTextColor = AdminTextPrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        user.copy(
                            displayName = displayName.trim(),
                            grade = grade.trim(),
                            section = section.trim(),
                            department = department.trim(),
                            designation = designation.trim()
                        )
                    )
                },
                enabled = displayName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AdminBrand)
            ) {
                Text("Save Changes", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = AdminTextSecondary)
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
    var displayName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminCard,
        title = {
            Text("Register New Device IP", color = AdminTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it.trim() },
                    label = { Text("IP Address") },
                    placeholder = { Text("e.g. 192.168.1.100") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdminTextPrimary,
                        unfocusedTextColor = AdminTextPrimary
                    )
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it.trim().removePrefix("@") },
                    label = { Text("Associated User Handle") },
                    placeholder = { Text("e.g. rahul_verma") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdminTextPrimary,
                        unfocusedTextColor = AdminTextPrimary
                    )
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. Rahul Verma") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdminTextPrimary,
                        unfocusedTextColor = AdminTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(ip, handle, displayName) },
                enabled = ip.isNotBlank() && handle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AdminBrand)
            ) {
                Text("Register IP", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = AdminTextSecondary)
            }
        }
    )
}
