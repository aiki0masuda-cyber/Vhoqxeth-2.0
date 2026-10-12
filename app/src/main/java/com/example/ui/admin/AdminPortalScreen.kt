package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material.icons.filled.Storage
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

@Composable
fun AdminPortalScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val adminRepository = remember { AdminRepository(context) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Data lists
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var registeredIps by remember { mutableStateOf<List<RegisteredIpEntry>>(emptyList()) }
    var probes by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

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
            val probesRes = adminRepository.getConnectionProbes()

            users = usersRes.getOrDefault(emptyList())
            registeredIps = ipsRes.getOrDefault(emptyList())
            probes = probesRes.getOrDefault(emptyList())
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
                            "Probes (${probes.size})",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) AdminAmber else VhoqxethTextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.testTag("admin_tab_probes")
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
                2 -> DiagnosticsTabContent(
                    probes = probes,
                    onSendTestProbe = {
                        coroutineScope.launch {
                            adminRepository.createTestProbe()
                            refreshData()
                        }
                    }
                )
            }
        }
    }

    // Add User Dialog
    if (showAddUserDialog) {
        AddUserDialog(
            onDismiss = { showAddUserDialog = false },
            onConfirm = { name, customId, ip ->
                coroutineScope.launch {
                    val res = adminRepository.createUser(name, customId, ip)
                    if (res.isSuccess) {
                        statusMessage = "Created user @$customId"
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
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
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

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName,
                        color = VhoqxethTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${user.customUserId}",
                        color = VhoqxethCyanBright,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp).testTag("edit_user_${user.customUserId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = AdminAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).testTag("delete_user_${user.customUserId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AdminCrimson,
                        modifier = Modifier.size(18.dp)
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
private fun DiagnosticsTabContent(
    probes: List<Map<String, Any>>,
    onSendTestProbe: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AdminCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, VhoqxethBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DATABASE STATUS",
                    color = AdminAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🟢 Firestore Connected • Online",
                    color = VhoqxethOnlineGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onSendTestProbe,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VhoqxethPrimaryViolet)
                ) {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Audit Ping", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "PROBE LOG HISTORY (${probes.size})",
            color = VhoqxethTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        probes.forEach { probe ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSurface),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, VhoqxethBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "ID: ${probe["_id"] ?: "probe"}",
                        color = VhoqxethCyanBright,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Status: ${probe["status"] ?: "OK"} • Client: ${probe["clientApp"] ?: "unknown"}",
                        color = VhoqxethTextPrimary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Timestamp: ${probe["timestamp"] ?: "N/A"}",
                        color = VhoqxethTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AddUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, customId: String, ip: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var customId by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        title = { Text("Add New User Account", color = AdminAmber, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                onClick = { onConfirm(name, customId, ip) },
                enabled = name.isNotBlank() && customId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AdminAmber)
            ) {
                Text("Create User", color = Color.Black, fontWeight = FontWeight.Bold)
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
