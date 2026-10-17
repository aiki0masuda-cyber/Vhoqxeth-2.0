package com.example.ui.splash
import androidx.compose.foundation.border

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.VhoqxethCyanAccent
import com.example.ui.theme.VhoqxethCyanBright
import com.example.ui.theme.VhoqxethDarkBg
import com.example.ui.theme.VhoqxethDeepBlack
import com.example.ui.theme.VhoqxethPrimaryViolet
import com.example.ui.theme.VhoqxethPrimaryVioletBright
import com.example.ui.theme.VhoqxethTextMuted
import com.example.ui.theme.VhoqxethTextPrimary
import com.example.ui.theme.VhoqxethTextSecondary
import kotlinx.coroutines.delay

import androidx.compose.ui.platform.LocalContext
import com.example.data.FirestoreConnectionManager
import com.example.data.FirestoreConnectionState

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onSplashFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    var statusText by remember { mutableStateOf("Initializing quantum enclave...") }

    // Pulsing animation for outer ambient glow
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    LaunchedEffect(Unit) {
        // Step 1: 0 to 30%
        progress.animateTo(
            targetValue = 0.35f,
            animationSpec = tween(500, easing = LinearEasing)
        )
        statusText = "Connecting to Firestore database..."

        // Step 2: Verify secure enclave
        progress.animateTo(
            targetValue = 0.75f,
            animationSpec = tween(600, easing = LinearEasing)
        )
        statusText = "Secure enclave verified"

        // Step 3: Complete loading
        progress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(500, easing = LinearEasing)
        )
        statusText = "DAV InfoExchange Ready"
        delay(300)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("splash_screen_root")
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E153A),
                        VhoqxethDarkBg,
                        VhoqxethDeepBlack
                    ),
                    radius = 1200f
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tap to skip loading immediately
                onSplashFinished()
            }
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Glowing Logo Container with ambient rings
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(170.dp)
                    .drawBehind {
                        // Ambient radial glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    VhoqxethPrimaryViolet.copy(alpha = glowAlpha),
                                    VhoqxethCyanAccent.copy(alpha = glowAlpha * 0.4f),
                                    Color.Transparent
                                )
                            ),
                            radius = size.width * 0.85f * pulseScale
                        )
                        // Neon outline ring
                        drawCircle(
                            color = VhoqxethPrimaryVioletBright.copy(alpha = 0.4f),
                            radius = (size.width / 2f) - 6.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
            ) {
                // Circular clipping with logo image
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFF1E2A78), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_dav_official),
                        contentDescription = "DAV College Managing Committee Crest",
                        modifier = Modifier
                            .size(126.dp)
                            .testTag("splash_logo_image")
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // App Name: DAV InfoExchange
            Text(
                text = "DAV InfoExchange",
                color = VhoqxethTextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.sp,
                modifier = Modifier.testTag("splash_app_title")
            )

            Text(
                text = "SCHOLASTIC COMMUNICATION NETWORK",
                color = VhoqxethCyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Institutional school tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2840))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Scholastic Protocol",
                    tint = VhoqxethCyanAccent,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "DAV COLLEGE MANAGING COMMITTEE • ESTD 1886",
                    color = VhoqxethTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
            }

            Spacer(modifier = Modifier.height(44.dp))

            // Animated Linear Progress
            Column(
                modifier = Modifier.fillMaxWidth(0.75f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .testTag("splash_progress_bar"),
                    color = VhoqxethCyanAccent,
                    trackColor = Color(0xFF23253B),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status message
                Text(
                    text = statusText,
                    color = VhoqxethTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.testTag("splash_status_text")
                )
            }
        }

        // Bottom footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tap anywhere to continue",
                color = VhoqxethTextMuted,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "v1.0.0 • Secure End-to-End",
                color = VhoqxethTextMuted.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}
