package com.example.ui.chat

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiImageGeneratorEngine
import com.example.ui.theme.DavAccentSky
import com.example.ui.theme.DavBorder
import com.example.ui.theme.DavBrandRoyalBlue
import com.example.ui.theme.DavCanvasLight
import com.example.ui.theme.DavSurfaceElevated
import com.example.ui.theme.DavTextMuted
import com.example.ui.theme.DavTextPrimary
import com.example.ui.theme.DavTextSecondary
import kotlinx.coroutines.launch

@Composable
fun AiGenerateImageDialog(
    initialPrompt: String = "",
    title: String = "Generate Study Illustration with AI",
    confirmButtonLabel: String = "Send into Chat",
    onDismiss: () -> Unit,
    onImageAccepted: (Bitmap, String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf(initialPrompt) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickChips = listOf(
        "🧬 Biology: Plant Cell Organelles Diagram" to "Detailed schematic cross-section of a plant cell with labeled chloroplasts, cell wall, and nucleus, educational textbook illustration",
        "⚛️ Physics: Ray Optics Prism Dispersion" to "Scientific diagram of light refraction and dispersion through a glass triangular prism showing spectrum of rainbow colors",
        "📐 Geometry: Pythagorean Theorem Proof" to "Geometric visual proof diagram of Pythagorean theorem with right triangle and squares on hypotenuse",
        "📜 History: Harappan Civilization Seal" to "Scholastic archeological sketch of Indus Valley Harappan terracotta unicorn seal with ancient pictographic script",
        "⚗️ Chemistry: Molecular Structure of Water" to "3D ball and stick molecular model diagram of water H2O with polar bonds and hydrogen bonds",
        "🛡️ School Emblem: DAV Academic Crest" to "Prestigious Vedic and scholastic emblem crest with flaming torch of knowledge, open textbook, and Vedic swastika motifs"
    )

    fun startGeneration() {
        if (prompt.trim().isBlank()) return
        isGenerating = true
        errorMessage = null
        coroutineScope.launch {
            val result = AiImageGeneratorEngine.generateImage(prompt.trim())
            isGenerating = false
            if (result.isSuccess) {
                generatedBitmap = result.getOrNull()
            } else {
                val ex = result.exceptionOrNull()
                val rawMsg = ex?.localizedMessage ?: ex?.message ?: "Failed to generate image."
                errorMessage = when {
                    rawMsg.contains("firebasevertexai.googleapis.com", ignoreCase = true) ||
                    rawMsg.contains("ServiceDisabledException", ignoreCase = true) -> {
                        "Vertex AI in Firebase was just provisioned! Cloud API propagation usually takes 1–2 minutes. Please tap Generate again in a moment."
                    }
                    rawMsg.contains("quota", ignoreCase = true) -> {
                        "AI generation rate limit reached. Please try again shortly."
                    }
                    else -> rawMsg
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isGenerating) onDismiss()
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DavAccentSky.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = DavBrandRoyalBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = DavTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Powered by Firebase AI Logic (gemini-nano-banana-2.1)",
                        color = DavTextSecondary,
                        fontSize = 10.sp
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    enabled = !isGenerating,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = DavTextMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (generatedBitmap == null) {
                    Text(
                        text = "Enter a concept description or choose a scholastic inspiration chip below:",
                        color = DavTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = {
                            prompt = it
                            errorMessage = null
                        },
                        placeholder = {
                            Text(
                                text = "e.g. Structure of DNA double helix with colored nitrogenous base pairs...",
                                color = DavTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        maxLines = 4,
                        minLines = 3,
                        enabled = !isGenerating,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DavCanvasLight,
                            unfocusedContainerColor = DavCanvasLight,
                            focusedBorderColor = DavBrandRoyalBlue,
                            unfocusedBorderColor = DavBorder,
                            focusedTextColor = DavTextPrimary,
                            unfocusedTextColor = DavTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_image_prompt_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "SCHOLASTIC INSPIRATION",
                        color = DavBrandRoyalBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizontal chips row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickChips.forEach { (label, fullPrompt) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DavSurfaceElevated)
                                    .border(1.dp, DavBorder, RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isGenerating) {
                                        prompt = fullPrompt
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = DavTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // Generated Bitmap Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, DavBorder, RoundedCornerShape(14.dp))
                            .background(DavCanvasLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = generatedBitmap!!.asImageBitmap(),
                            contentDescription = "AI Generated Artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_generated_image_preview")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Prompt: \"$prompt\"",
                        color = DavTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }

                // Loading State indicator
                AnimatedVisibility(visible = isGenerating) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            color = DavBrandRoyalBlue,
                            trackColor = DavSurfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Synthesizing visual representation with Gemini Nano Banana...",
                            color = DavBrandRoyalBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Educational synthesis typically completes within 30–45s",
                            color = DavTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Error Message Display
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFB91C1C),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (generatedBitmap != null) {
                Button(
                    onClick = {
                        onImageAccepted(generatedBitmap!!, prompt.trim())
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DavBrandRoyalBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_ai_image_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(confirmButtonLabel, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { startGeneration() },
                    enabled = prompt.trim().isNotBlank() && !isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DavBrandRoyalBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("start_ai_generation_button")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Synthesizing...")
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (generatedBitmap != null) {
                OutlinedButton(
                    onClick = {
                        generatedBitmap = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("regenerate_ai_image_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Prompt")
                }
            } else {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
