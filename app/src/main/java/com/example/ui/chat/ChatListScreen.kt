package com.example.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DummyData
import com.example.data.FirestoreConnectionManager
import com.example.data.FirestoreConnectionState
import com.example.model.ActiveContact
import com.example.model.ConversationCategory
import com.example.model.ConversationThread
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
import com.example.model.LegalContent
import com.example.model.UserProfile
import com.example.ui.legal.AboutAppSheet
import com.example.ui.legal.LegalViewerSheet
import com.example.ui.theme.VhoqxethTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    userProfile: UserProfile? = null,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Firestore connection manager & live state
    val connectionManager = remember { FirestoreConnectionManager(context) }
    var firestoreState by remember { mutableStateOf<FirestoreConnectionState>(FirestoreConnectionState.Connecting) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showConnectionDialog by remember { mutableStateOf(false) }

    // Initial connection probe on screen load
    LaunchedEffect(Unit) {
        firestoreState = FirestoreConnectionState.Connecting
        firestoreState = connectionManager.testDatabaseConnection()
    }

    // Search and filtering state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ConversationCategory.ALL) }

    // Bottom Navigation selection
    var selectedBottomNavIndex by remember { mutableIntStateOf(0) }

    // Selected conversation for preview modal
    var selectedThread by remember { mutableStateOf<ConversationThread?>(null) }
    val threadBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // New Chat Action bottom sheet
    var showNewChatSheet by remember { mutableStateOf(false) }
    val newChatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Three-dot menu and legal modal states
    var showMenuDropdown by remember { mutableStateOf(false) }
    var showAboutAppSheet by remember { mutableStateOf(false) }
    var showPrivacyPolicySheet by remember { mutableStateOf(false) }
    var showTermsSheet by remember { mutableStateOf(false) }

    // Handle back button when search or sheets are active
    BackHandler(
        enabled = isSearchActive || selectedThread != null || showNewChatSheet ||
            showProfileSheet || showAboutAppSheet || showPrivacyPolicySheet || showTermsSheet
    ) {
        when {
            showAboutAppSheet -> showAboutAppSheet = false
            showPrivacyPolicySheet -> showPrivacyPolicySheet = false
            showTermsSheet -> showTermsSheet = false
            selectedThread != null -> selectedThread = null
            showNewChatSheet -> showNewChatSheet = false
            showProfileSheet -> showProfileSheet = false
            isSearchActive -> {
                isSearchActive = false
                searchQuery = ""
            }
        }
    }

    // Filter threads based on category and search query
    val filteredThreads = remember(searchQuery, selectedFilter) {
        DummyData.placeholderThreads.filter { thread ->
            val matchesCategory = when (selectedFilter) {
                ConversationCategory.ALL -> true
                ConversationCategory.UNREAD -> thread.unreadCount > 0
                ConversationCategory.DIRECT -> thread.category == ConversationCategory.DIRECT
                ConversationCategory.GROUP -> thread.category == ConversationCategory.GROUP
                ConversationCategory.CHANNEL -> thread.category == ConversationCategory.CHANNEL
            }
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                thread.name.contains(searchQuery, ignoreCase = true) ||
                    thread.lastMessage.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("chat_list_screen_scaffold"),
        containerColor = VhoqxethDarkBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                // PROFILE PICTURE IN LEFT
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(42.dp)
                            .clickable { showProfileSheet = true }
                            .testTag("profile_avatar_left"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(VhoqxethPrimaryViolet)
                                .border(1.dp, VhoqxethBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile?.displayName?.take(2)?.uppercase() ?: "VQ",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Online indicator dot
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(VhoqxethDarkBg)
                                .padding(1.5.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(VhoqxethOnlineGreen)
                            )
                        }
                    }
                },
                // APP NAME IN MIDDLE
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.testTag("app_name_middle_container")
                    ) {
                        Text(
                            text = "Vhoqxeth",
                            color = VhoqxethTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 1.sp,
                            modifier = Modifier.testTag("chat_app_bar_title")
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (firestoreState is FirestoreConnectionState.Connected) {
                                            VhoqxethOnlineGreen
                                        } else {
                                            VhoqxethPrimaryViolet
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (firestoreState is FirestoreConnectionState.Connected) {
                                    "Firestore Connected"
                                } else {
                                    "Connecting to DB..."
                                },
                                color = VhoqxethCyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                // ACTIONS IN RIGHT (Firestore DB Status & Search)
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                firestoreState = FirestoreConnectionState.Connecting
                                val res = connectionManager.testDatabaseConnection()
                                firestoreState = res
                                val msg = if (res is FirestoreConnectionState.Connected) {
                                    "Firestore Verified (${res.latencyMs}ms): 200 OK"
                                } else {
                                    "Firestore Request Sent"
                                }
                                snackbarHostState.showSnackbar(msg)
                            }
                        },
                        modifier = Modifier.testTag("firestore_status_button")
                    ) {
                        Icon(
                            imageVector = when (firestoreState) {
                                is FirestoreConnectionState.Connected -> Icons.Default.CloudDone
                                is FirestoreConnectionState.Connecting -> Icons.Default.CloudSync
                                else -> Icons.Default.CloudSync
                            },
                            contentDescription = "Firestore Status",
                            tint = if (firestoreState is FirestoreConnectionState.Connected) {
                                VhoqxethCyanAccent
                            } else {
                                VhoqxethPrimaryVioletBright
                            }
                        )
                    }

                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        },
                        modifier = Modifier.testTag("search_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = VhoqxethTextPrimary
                        )
                    }

                    // Three-dot overflow menu (About the App, Privacy Policy, Terms & Conditions)
                    Box {
                        IconButton(
                            onClick = { showMenuDropdown = true },
                            modifier = Modifier.testTag("overflow_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = VhoqxethTextPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false },
                            modifier = Modifier
                                .background(VhoqxethSurfaceElevated)
                                .border(1.dp, VhoqxethBorder, RoundedCornerShape(8.dp))
                                .testTag("top_nav_overflow_dropdown")
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "About the App",
                                        color = VhoqxethTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = VhoqxethCyanAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showMenuDropdown = false
                                    showAboutAppSheet = true
                                },
                                modifier = Modifier.testTag("menu_item_about_app")
                            )

                            HorizontalDivider(color = VhoqxethBorder, thickness = 0.5.dp)

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Privacy Policy",
                                        color = VhoqxethTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Policy,
                                        contentDescription = null,
                                        tint = VhoqxethTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showMenuDropdown = false
                                    showPrivacyPolicySheet = true
                                },
                                modifier = Modifier.testTag("menu_item_privacy_policy")
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Terms & Conditions",
                                        color = VhoqxethTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Gavel,
                                        contentDescription = null,
                                        tint = VhoqxethTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showMenuDropdown = false
                                    showTermsSheet = true
                                },
                                modifier = Modifier.testTag("menu_item_terms_conditions")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = VhoqxethDarkBg
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = VhoqxethSurface,
                contentColor = VhoqxethTextPrimary,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("main_bottom_nav_bar")
            ) {
                val navItems = listOf(
                    Triple("Chats", Icons.AutoMirrored.Filled.Chat, 8),
                    Triple("Calls", Icons.Default.Call, 0),
                    Triple("Channels", Icons.Default.Stream, 0),
                    Triple("Vault", Icons.Default.Lock, 0),
                    Triple("Settings", Icons.Default.Settings, 0)
                )

                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedBottomNavIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            selectedBottomNavIndex = index
                            if (index != 0) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("${item.first} section ready")
                                }
                            }
                        },
                        icon = {
                            if (item.third > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = VhoqxethPrimaryViolet,
                                            contentColor = Color.White
                                        ) {
                                            Text(text = item.third.toString(), fontSize = 10.sp)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = item.second,
                                        contentDescription = item.first,
                                        tint = if (isSelected) VhoqxethCyanAccent else VhoqxethTextMuted
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = item.second,
                                    contentDescription = item.first,
                                    tint = if (isSelected) VhoqxethCyanAccent else VhoqxethTextMuted
                                )
                            }
                        },
                        label = {
                            Text(
                                text = item.first,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) VhoqxethCyanAccent else VhoqxethTextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = VhoqxethSurfaceElevated
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatSheet = true },
                containerColor = VhoqxethPrimaryViolet,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("new_chat_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Conversation",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive Search Field (if toggled)
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("Search threads or contacts...", color = VhoqxethTextMuted)
                        },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = VhoqxethCyanAccent
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = VhoqxethTextMuted
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input_field"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = VhoqxethSurface,
                            unfocusedContainerColor = VhoqxethSurface,
                            focusedBorderColor = VhoqxethCyanAccent,
                            unfocusedBorderColor = VhoqxethBorder,
                            focusedTextColor = VhoqxethTextPrimary,
                            unfocusedTextColor = VhoqxethTextPrimary
                        )
                    )
                }
            }

            // FIRESTORE CONNECTION CONFIRMATION BANNER
            FirestoreConnectionBanner(
                state = firestoreState,
                onSendProbe = {
                    coroutineScope.launch {
                        firestoreState = FirestoreConnectionState.Connecting
                        firestoreState = connectionManager.testDatabaseConnection()
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Active Contacts & Stories Horizontal Row
            ActiveContactsRow(
                contacts = DummyData.activeContacts,
                onAddStatusClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Publishing encrypted story broadcast...")
                    }
                },
                onContactClick = { contact ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Direct thread ready for ${contact.name}")
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            FilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Conversation Threads List
            if (filteredThreads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "No results",
                            tint = VhoqxethTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No conversations found",
                            color = VhoqxethTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your search terms or filter category",
                            color = VhoqxethTextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("conversation_threads_list"),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = filteredThreads,
                        key = { it.id }
                    ) { thread ->
                        ChatItemRow(
                            thread = thread,
                            onClick = { selectedThread = thread }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = VhoqxethBorder.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Profile View (Opened from Left Avatar)
    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            containerColor = VhoqxethSurfaceElevated,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(VhoqxethBorder)
                )
            }
        ) {
            ProfileDetailsSheet(
                databaseId = connectionManager.databaseId,
                firestoreState = firestoreState,
                userProfile = userProfile,
                onSignOut = onSignOut,
                onDismiss = { showProfileSheet = false }
            )
        }
    }

    // Modal Bottom Sheet: Conversation Details / Quick Actions
    selectedThread?.let { thread ->
        ModalBottomSheet(
            onDismissRequest = { selectedThread = null },
            sheetState = threadBottomSheetState,
            containerColor = VhoqxethSurfaceElevated,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(VhoqxethBorder)
                )
            }
        ) {
            ConversationDetailsSheetContent(
                thread = thread,
                onAction = { actionName ->
                    selectedThread = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("$actionName for ${thread.name}")
                    }
                }
            )
        }
    }

    // Modal Bottom Sheet: New Chat Options
    if (showNewChatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNewChatSheet = false },
            sheetState = newChatSheetState,
            containerColor = VhoqxethSurfaceElevated,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(VhoqxethBorder)
                )
            }
        ) {
            NewChatSheetContent(
                onAction = { actionName ->
                    showNewChatSheet = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Launched: $actionName")
                    }
                }
            )
        }
    }

    // Modal Bottom Sheet: About the App (features listed, admin panel NOT listed)
    if (showAboutAppSheet) {
        AboutAppSheet(
            onDismiss = { showAboutAppSheet = false }
        )
    }

    // Modal Bottom Sheet: Privacy Policy
    if (showPrivacyPolicySheet) {
        LegalViewerSheet(
            title = "Privacy Policy",
            subtitle = "PROTOCOL DATA GOVERNANCE",
            sections = LegalContent.privacyPolicySections,
            onDismiss = { showPrivacyPolicySheet = false }
        )
    }

    // Modal Bottom Sheet: Terms & Conditions
    if (showTermsSheet) {
        LegalViewerSheet(
            title = "Terms & Conditions",
            subtitle = "DECENTRALIZED PROTOCOL CHARTER",
            sections = LegalContent.termsAndConditionsSections,
            onDismiss = { showTermsSheet = false }
        )
    }
}

@Composable
private fun FirestoreConnectionBanner(
    state: FirestoreConnectionState,
    onSendProbe: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VhoqxethSurface)
            .border(1.dp, VhoqxethBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("firestore_connection_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (state is FirestoreConnectionState.Connected) {
                            VhoqxethOnlineGreen.copy(alpha = 0.15f)
                        } else {
                            VhoqxethPrimaryViolet.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (state is FirestoreConnectionState.Connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = VhoqxethCyanAccent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (state is FirestoreConnectionState.Connected) Icons.Default.CheckCircle else Icons.Default.CloudSync,
                        contentDescription = "Status",
                        tint = if (state is FirestoreConnectionState.Connected) VhoqxethCyanAccent else VhoqxethPrimaryVioletBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Firestore Database",
                        color = VhoqxethTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state is FirestoreConnectionState.Connected) "CONNECTED" else "CONNECTING",
                        color = if (state is FirestoreConnectionState.Connected) VhoqxethOnlineGreen else VhoqxethCyanBright,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (state) {
                        is FirestoreConnectionState.Connected -> "Latency: ${state.latencyMs}ms • Ping verified"
                        is FirestoreConnectionState.Connecting -> "Sending probe request to database..."
                        is FirestoreConnectionState.Error -> "Probe error: ${state.message}"
                        else -> "Ready"
                    },
                    color = VhoqxethTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action button to send request to Firestore database
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(VhoqxethPrimaryViolet.copy(alpha = 0.2f))
                    .border(1.dp, VhoqxethPrimaryVioletBright, RoundedCornerShape(8.dp))
                    .clickable(onClick = onSendProbe)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("ping_firestore_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Ping",
                        tint = VhoqxethCyanAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Probe",
                        color = VhoqxethTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailsSheet(
    databaseId: String,
    firestoreState: FirestoreConnectionState,
    userProfile: UserProfile?,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(VhoqxethPrimaryViolet),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userProfile?.displayName?.take(2)?.uppercase() ?: "VQ",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = userProfile?.displayName ?: "Vhoqxeth Identity",
            color = VhoqxethTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (userProfile?.customUserId?.isNotEmpty() == true) "@${userProfile.customUserId}" else "Quantum Encrypted Device Profile",
            color = VhoqxethCyanBright,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VhoqxethSurface)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (userProfile?.currentIp?.isNotEmpty() == true) {
                    Text(
                        text = "ACTIVE DEVICE IP",
                        color = VhoqxethTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = userProfile.currentIp,
                        color = VhoqxethCyanAccent,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    HorizontalDivider(color = VhoqxethBorder, thickness = 0.5.dp)
                }

                if (!userProfile?.associatedIps.isNullOrEmpty()) {
                    Text(
                        text = "LINKED NETWORK IPS (${userProfile!!.associatedIps.size})",
                        color = VhoqxethTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = userProfile.associatedIps.joinToString(", "),
                        color = VhoqxethTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    HorizontalDivider(color = VhoqxethBorder, thickness = 0.5.dp)
                }

                Text(
                    text = "FIRESTORE DATABASE TARGET",
                    color = VhoqxethTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = databaseId,
                    color = VhoqxethCyanBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                HorizontalDivider(color = VhoqxethBorder, thickness = 0.5.dp)
                Text(
                    text = "CONNECTION STATUS",
                    color = VhoqxethTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (firestoreState is FirestoreConnectionState.Connected) {
                        "🟢 Connected • Verified at ${firestoreState.confirmedTimestamp}"
                    } else {
                        "🟡 Synchronizing with backend"
                    },
                    color = VhoqxethTextPrimary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        SheetActionButton(
            label = "Sign Out / Switch Identity",
            icon = Icons.Default.Close,
            isPrimary = false,
            onClick = {
                onDismiss()
                onSignOut()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SheetActionButton(
            label = "Close Details",
            icon = Icons.Default.CheckCircle,
            isPrimary = true,
            onClick = onDismiss
        )
    }
}

@Composable
private fun ActiveContactsRow(
    contacts: List<ActiveContact>,
    onAddStatusClick: () -> Unit,
    onContactClick: (ActiveContact) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_contacts_row"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // "My Story / New Status" Item
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = onAddStatusClick)
                    .width(62.dp)
            ) {
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(VhoqxethSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Status",
                            tint = VhoqxethCyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "My Key",
                    color = VhoqxethTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Contact Avatars with glowing status ring
        items(contacts, key = { it.id }) { contact ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onContactClick(contact) }
                    .width(62.dp)
            ) {
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer gradient ring if has active story
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                if (contact.hasStory) {
                                    Brush.sweepGradient(
                                        listOf(
                                            VhoqxethPrimaryViolet,
                                            VhoqxethCyanAccent,
                                            VhoqxethPrimaryVioletBright,
                                            VhoqxethPrimaryViolet
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(VhoqxethBorder, VhoqxethBorder)
                                    )
                                }
                            )
                            .padding(2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner circle avatar
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(contact.colorStart), Color(contact.colorEnd))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.initials,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Online indicator
                    if (contact.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(VhoqxethDarkBg)
                                .padding(1.5.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(VhoqxethOnlineGreen)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = contact.name.split(" ").firstOrNull() ?: contact.name,
                    color = VhoqxethTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: ConversationCategory,
    onFilterSelected: (ConversationCategory) -> Unit
) {
    val filters = listOf(
        Pair(ConversationCategory.ALL, "All"),
        Pair(ConversationCategory.UNREAD, "Unread"),
        Pair(ConversationCategory.DIRECT, "Direct"),
        Pair(ConversationCategory.GROUP, "Groups"),
        Pair(ConversationCategory.CHANNEL, "Channels")
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("filter_chips_row"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (category, title) ->
            val isSelected = selectedFilter == category
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) {
                            VhoqxethPrimaryViolet
                        } else {
                            VhoqxethSurface
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) VhoqxethPrimaryVioletBright else VhoqxethBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onFilterSelected(category) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else VhoqxethTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ConversationDetailsSheetContent(
    thread: ConversationThread,
    onAction: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Avatar
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(thread.avatarColorStart), Color(thread.avatarColorEnd))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = thread.avatarInitials,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = thread.name,
            color = VhoqxethTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = VhoqxethCyanAccent,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "End-to-End Encrypted via Curve25519",
                color = VhoqxethCyanAccent,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Last snippet quote box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VhoqxethSurface)
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "LAST RECEIVED DISPATCH",
                    color = VhoqxethTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = thread.lastMessage,
                    color = VhoqxethTextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SheetActionButton(
                label = "Open Encrypted Thread",
                icon = Icons.AutoMirrored.Filled.Chat,
                isPrimary = true,
                onClick = { onAction("Opening Conversation") }
            )

            SheetActionButton(
                label = if (thread.isMuted) "Unmute Notifications" else "Mute Notifications",
                icon = Icons.Default.NotificationsOff,
                isPrimary = false,
                onClick = { onAction("Toggled Mute") }
            )

            SheetActionButton(
                label = "Mark as Read",
                icon = Icons.Default.DoneAll,
                isPrimary = false,
                onClick = { onAction("Marked as Read") }
            )
        }
    }
}

@Composable
private fun NewChatSheetContent(
    onAction: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Initiate Encrypted Transmission",
            color = VhoqxethTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Choose channel type to open quantum handshake",
            color = VhoqxethTextMuted,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetActionButton(
                label = "New Direct Contact",
                icon = Icons.Default.PersonAdd,
                isPrimary = true,
                onClick = { onAction("New Direct Message") }
            )

            SheetActionButton(
                label = "New Mesh Group",
                icon = Icons.Default.GroupAdd,
                isPrimary = false,
                onClick = { onAction("New Group") }
            )

            SheetActionButton(
                label = "New Encrypted Channel",
                icon = Icons.Default.WifiTethering,
                isPrimary = false,
                onClick = { onAction("New Broadcast Channel") }
            )

            SheetActionButton(
                label = "Scan Contact Public Key (QR)",
                icon = Icons.Default.QrCodeScanner,
                isPrimary = false,
                onClick = { onAction("Scan Key QR") }
            )
        }
    }
}

@Composable
private fun SheetActionButton(
    label: String,
    icon: ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPrimary) VhoqxethPrimaryViolet else VhoqxethSurface
            )
            .border(
                width = 1.dp,
                color = if (isPrimary) VhoqxethPrimaryVioletBright else VhoqxethBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPrimary) Color.White else VhoqxethCyanAccent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = if (isPrimary) Color.White else VhoqxethTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
