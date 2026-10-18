package com.example.ui.chat

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserContact
import com.example.ui.theme.VhoqxethBorder
import com.example.ui.theme.VhoqxethBorderLightStrong
import com.example.ui.theme.VhoqxethCyanAccent
import com.example.ui.theme.VhoqxethDarkBg
import com.example.ui.theme.VhoqxethOnlineGreen
import com.example.ui.theme.VhoqxethPrimaryViolet
import com.example.ui.theme.VhoqxethSurface
import com.example.ui.theme.VhoqxethSurfaceElevated
import com.example.ui.theme.VhoqxethTextMuted
import com.example.ui.theme.VhoqxethTextPrimary
import com.example.ui.theme.VhoqxethTextSecondary

@Composable
fun ContactsTabContent(
    contacts: List<UserContact>,
    onContactSelected: (UserContact) -> Unit,
    onAddContactClick: () -> Unit,
    onAddToGroupClick: ((UserContact) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.displayName.contains(searchQuery, ignoreCase = true) ||
                    contact.contactUserId.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VhoqxethDarkBg)
            .testTag("contacts_tab_content")
    ) {
        // Search & Add Contact Header Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search contacts by name or @id...", color = VhoqxethTextMuted, fontSize = 13.sp)
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
                    .testTag("contacts_search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VhoqxethSurface,
                    unfocusedContainerColor = VhoqxethSurface,
                    focusedBorderColor = VhoqxethCyanAccent,
                    unfocusedBorderColor = VhoqxethBorder,
                    focusedTextColor = VhoqxethTextPrimary,
                    unfocusedTextColor = VhoqxethTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Roster Info & Add Contact Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REGISTERED PEERS (${contacts.size})",
                    color = VhoqxethTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = onAddContactClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VhoqxethPrimaryViolet,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("contacts_add_peer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Add Peer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Contact List or Empty States
        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(VhoqxethSurface)
                        .border(1.dp, VhoqxethBorder, RoundedCornerShape(18.dp))
                        .padding(24.dp)
                        .testTag("contacts_empty_card")
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(VhoqxethPrimaryViolet.copy(alpha = 0.15f))
                            .border(1.5.dp, VhoqxethPrimaryViolet, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "No contacts",
                            tint = VhoqxethCyanAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Contacts Added Yet",
                        color = VhoqxethTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Add peers by their Custom User ID (@handle) to establish end-to-end encrypted direct messaging channels.",
                        color = VhoqxethTextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onAddContactClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("empty_state_add_peer_cta"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VhoqxethPrimaryViolet,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Contact by User ID",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = VhoqxethTextMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No contacts match '$searchQuery'",
                        color = VhoqxethTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Check the spelling or try searching by User ID handle",
                        color = VhoqxethTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("contacts_roster_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredContacts, key = { it.contactUserId }) { contact ->
                    ContactCardItem(
                        contact = contact,
                        onMessageClick = { onContactSelected(contact) },
                        onAddToGroupClick = onAddToGroupClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactCardItem(
    contact: UserContact,
    onMessageClick: () -> Unit,
    onAddToGroupClick: ((UserContact) -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VhoqxethSurface)
            .border(1.dp, VhoqxethBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onMessageClick)
            .padding(14.dp)
            .testTag("contact_item_${contact.contactUserId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(contact.avatarColorStart),
                                Color(contact.avatarColorEnd)
                            )
                        )
                    )
                    .border(1.dp, VhoqxethBorderLightStrong, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.initials,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Online indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(VhoqxethOnlineGreen)
                        .border(1.5.dp, VhoqxethSurface, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    color = VhoqxethTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${contact.contactUserId}",
                        color = VhoqxethCyanAccent,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(VhoqxethSurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = VhoqxethCyanAccent,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "E2EE PEER",
                                color = VhoqxethTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Add to Group button
            if (onAddToGroupClick != null) {
                IconButton(
                    onClick = { onAddToGroupClick(contact) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(VhoqxethPrimaryViolet.copy(alpha = 0.12f))
                        .testTag("contact_add_to_group_button_${contact.contactUserId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = "Add @${contact.contactUserId} to Group",
                        tint = VhoqxethPrimaryViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Chat button
            IconButton(
                onClick = onMessageClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(VhoqxethPrimaryViolet.copy(alpha = 0.2f))
                    .testTag("contact_message_button_${contact.contactUserId}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Message @${contact.contactUserId}",
                    tint = VhoqxethCyanAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
