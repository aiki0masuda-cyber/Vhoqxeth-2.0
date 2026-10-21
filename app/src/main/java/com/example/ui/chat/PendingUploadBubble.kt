package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.DavAccentSky
import com.example.ui.theme.DavBorder
import com.example.ui.theme.DavBrandRoyalBlue
import com.example.ui.theme.DavSurfaceElevated
import com.example.ui.theme.DavTextMuted
import com.example.ui.theme.DavTextPrimary
import com.example.ui.theme.DavTextSecondary
import com.example.upload.PendingUploadMessage
import com.example.upload.UploadState

/**
 * Optimistic Chat Message Item rendered in the conversation stream
 * while a file is being uploaded to Cloudflare R2 via pre-signed URL.
 *
 * Provides:
 * - Local image thumbnail or file type badge
 * - Real-time animated progress bar & percentage
 * - Current stage status (requesting pre-signed URL -> streaming to R2)
 * - Error state with Retry and Dismiss/Cancel actions
 */
@Composable
fun PendingUploadBubble(
    pendingMessage: PendingUploadMessage,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = pendingMessage.uploadState
    val isError = state is UploadState.Error

    val animatedProgress by animateFloatAsState(
        targetValue = when (state) {
            is UploadState.Uploading -> state.progress
            is UploadState.Success -> 1.0f
            else -> 0.05f
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "UploadProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Label indicating outgoing in-flight message
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
                tint = if (isError) Color(0xFFDC2626) else DavBrandRoyalBlue,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (state) {
                    is UploadState.RequestingUrl -> "Securing Cloudflare R2 link..."
                    is UploadState.Uploading -> "Streaming to R2 (${(animatedProgress * 100).toInt()}%)"
                    is UploadState.Success -> "Upload complete • Finalizing..."
                    is UploadState.Error -> "Upload interrupted"
                    is UploadState.Idle -> "Preparing upload..."
                },
                color = if (isError) Color(0xFFDC2626) else DavTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Main Bubble Card
        Box(
            modifier = Modifier
                .widthIn(min = 220.dp, max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 4.dp
                    )
                )
                .background(if (isError) Color(0xFFFEF2F2) else DavSurfaceElevated)
                .border(
                    width = 1.dp,
                    color = if (isError) Color(0xFFFCA5A5) else DavAccentSky.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 4.dp
                    )
                )
                .padding(12.dp)
                .testTag("pending_upload_bubble_${pendingMessage.uploadId}")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Top row: File Icon, Name, Size & Cancel button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isError) Color(0xFFFEE2E2) else DavBrandRoyalBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getUploadFileIcon(pendingMessage.fileType),
                            contentDescription = null,
                            tint = if (isError) Color(0xFFDC2626) else DavBrandRoyalBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pendingMessage.fileName,
                            color = DavTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pendingMessage.fileType} • ${pendingMessage.fileSizeFormatted}",
                            color = DavTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    // Cancel button
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel upload",
                            tint = DavTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // If image, show local thumbnail preview with progressive overlay
                if (pendingMessage.fileType.equals("IMAGE", ignoreCase = true)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = pendingMessage.localUri,
                            contentDescription = "Upload preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Translucent progress scrim
                        if (!isError) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(42.dp),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.3f),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }
                }

                // Caption if provided
                if (pendingMessage.caption.isNotBlank()) {
                    Text(
                        text = pendingMessage.caption,
                        color = DavTextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                // Progress Bar or Error State
                if (isError) {
                    val errorState = state as UploadState.Error
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = errorState.message,
                                color = Color(0xFF991B1B),
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Discard", fontSize = 11.sp, color = DavTextSecondary)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = onRetry,
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DavBrandRoyalBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Active upload progress
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (state is UploadState.RequestingUrl) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = DavAccentSky,
                                trackColor = DavBorder
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = DavBrandRoyalBlue,
                                trackColor = DavBorder,
                                strokeCap = StrokeCap.Round
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (state is UploadState.RequestingUrl) "Contacting worker..." else "Pre-signed PUT",
                                color = DavTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                color = DavBrandRoyalBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getUploadFileIcon(fileType: String): ImageVector {
    return when (fileType.uppercase()) {
        "IMAGE" -> Icons.Default.Image
        "PDF" -> Icons.Default.PictureAsPdf
        "VIDEO" -> Icons.Default.Videocam
        else -> Icons.Default.Description
    }
}
