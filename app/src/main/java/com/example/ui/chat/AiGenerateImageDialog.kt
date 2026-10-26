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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiImageGeneratorEngine
import com.example.ai.ImageAspectRatio
import com.example.ai.SiliconFlowImageEngine
import com.example.ai.SiliconFlowModel
import com.example.data.SessionManager
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
    initialInputBitmap: Bitmap? = null,
    title: String = "Generate Study Illustration with AI",
    confirmButtonLabel: String = "Send into Chat",
    onDismiss: () -> Unit,
    onImageAccepted: (Bitmap, String) -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var prompt by remember { mutableStateOf(initialPrompt) }
    var selectedAspectRatio by remember { mutableStateOf(ImageAspectRatio.SQUARE_1_1) }
    var selectedModel by remember {
        mutableStateOf(SiliconFlowModel.fromId(sessionManager.getSiliconFlowModel()))
    }

    var customApiKey by remember { mutableStateOf(sessionManager.getSiliconFlowApiKey()) }
    var showSettings by remember { mutableStateOf(false) }

    var isGenerating by remember { mutableStateOf(false) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(initialInputBitmap) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val secretsPanelKey = remember { SiliconFlowImageEngine.resolveConfiguredApiKey() }
    val effectiveToken = customApiKey.trim().ifBlank { secretsPanelKey }
    val isKeyActive = effectiveToken.isNotBlank()

    val quickChips = listOf(
        "🧬 Biology: Plant Cell Organelles" to "Cross-section of a plant cell with labeled chloroplasts, cell wall, and nucleus",
        "⚛️ Physics: Prism Light Dispersion" to "Scientific diagram of light refraction and dispersion through glass triangular prism",
        "📐 Geometry: Pythagorean Proof" to "Geometric proof diagram of Pythagorean theorem with right triangle and squares",
        "📜 History: Harappan Unicorn Seal" to "Scholastic archeological sketch of Indus Valley Harappan seal",
        "⚗️ Chemistry: Water Molecule Model" to "3D molecular model diagram of water H2O with polar bonds",
        "🪐 Astronomy: Solar System Orbits" to "High contrast diagram of planets orbiting the Sun with elliptical paths",
        "🛡️ School Crest: DAV Knowledge Crest" to "Prestigious Vedic and scholastic emblem crest with flaming torch of knowledge"
    )

    fun startGeneration() {
        if (prompt.trim().isBlank()) return
        isGenerating = true
        errorMessage = null
        coroutineScope.launch {
            val result = AiImageGeneratorEngine.generateImageWithFailover(
                prompt = prompt.trim(),
                aspectRatio = selectedAspectRatio,
                model = selectedModel,
                apiKey = customApiKey.trim()
            )
            isGenerating = false
            if (result.isSuccess) {
                val output = result.getOrThrow()
                generatedBitmap = output.bitmap
                if (output.wasFailover) {
                    selectedModel = output.usedModel
                    sessionManager.setSiliconFlowModel(output.usedModel.id)
                }
            } else {
                val ex = result.exceptionOrNull()
                errorMessage = ex?.localizedMessage ?: "Failed to generate image."
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
                        text = "SiliconFlow AI (${selectedModel.displayName})",
                        color = DavTextSecondary,
                        fontSize = 11.sp
                    )
                }
                IconButton(
                    onClick = { showSettings = !showSettings },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (showSettings) DavBrandRoyalBlue else DavTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
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
                // Token Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isKeyActive) Color(0xFFF0FDF4) else Color(0xFFFEF2F2))
                        .border(1.dp, if (isKeyActive) Color(0xFFBBF7D0) else Color(0xFFFECACA), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isKeyActive) Icons.Default.Shield else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (isKeyActive) Color(0xFF15803D) else Color(0xFFDC2626),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            customApiKey.isNotBlank() -> "Using In-App SiliconFlow Token"
                            secretsPanelKey.isNotBlank() -> "Using Secrets Panel Token"
                            else -> "Enter your SiliconFlow API key (sk-...) in settings"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isKeyActive) Color(0xFF166534) else Color(0xFF991B1B)
                    )
                }

                // Settings Accordion
                AnimatedVisibility(visible = showSettings) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DavSurfaceElevated)
                            .border(1.dp, DavBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = DavBrandRoyalBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SiliconFlow API Key",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DavTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter your SiliconFlow API key (starts with sk-). Saved securely on your device.",
                            fontSize = 10.sp,
                            color = DavTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = customApiKey,
                            onValueChange = {
                                val sanitized = SiliconFlowImageEngine.sanitizeApiKey(it)
                                customApiKey = sanitized
                                sessionManager.setSiliconFlowApiKey(sanitized)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text(if (secretsPanelKey.isNotBlank()) "●●●●●●●● (Loaded from Secrets Panel)" else "sk-xxxxxxxxxxxxxxxxxxxx", fontSize = 11.sp, color = DavTextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }
                }

                if (generatedBitmap == null) {
                    // Model Selection Chips
                    Text(
                        text = "SILICONFLOW DIFFUSION MODEL",
                        color = DavBrandRoyalBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SiliconFlowModel.entries.forEach { modelOption ->
                            val isSelected = selectedModel == modelOption
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DavBrandRoyalBlue else DavSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) DavBrandRoyalBlue else DavBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !isGenerating) {
                                        selectedModel = modelOption
                                        sessionManager.setSiliconFlowModel(modelOption.id)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("model_${modelOption.name}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (modelOption.isPrimary) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.Yellow else Color(0xFFD97706),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = modelOption.displayName,
                                        color = if (isSelected) Color.White else DavTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Enter a concept description or choose an inspiration chip below:",
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
                                text = "e.g. Educational diagram of human heart with labeled chambers...",
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

                    // Aspect ratio chips
                    Text(
                        text = "ASPECT RATIO",
                        color = DavBrandRoyalBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ImageAspectRatio.entries.forEach { ratio ->
                            val isSelected = selectedAspectRatio == ratio
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DavBrandRoyalBlue else DavSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) DavBrandRoyalBlue else DavBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !isGenerating) {
                                        selectedAspectRatio = ratio
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("aspect_ratio_${ratio.name}")
                            ) {
                                Text(
                                    text = ratio.label,
                                    color = if (isSelected) Color.White else DavTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "SCHOLASTIC INSPIRATION",
                        color = DavBrandRoyalBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

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
                    val ratio = selectedAspectRatio.ratioWidth / selectedAspectRatio.ratioHeight
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(ratio.coerceIn(0.56f, 1.78f))
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, DavBorder, RoundedCornerShape(14.dp))
                            .background(DavCanvasLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = generatedBitmap!!.asImageBitmap(),
                            contentDescription = "AI Generated Artwork",
                            contentScale = ContentScale.Fit,
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
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
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
                            text = "Synthesizing with SiliconFlow (${selectedModel.displayName})...",
                            color = DavBrandRoyalBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Error Notice
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notice",
                                color = Color(0xFF991B1B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFB91C1C),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showSettings = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DavBrandRoyalBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Configure API Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
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
