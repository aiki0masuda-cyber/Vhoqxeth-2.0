package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.AuthRepository
import com.example.data.DeviceIpManager
import com.example.data.SessionManager
import com.example.model.UserProfile
import com.example.ui.admin.AdminPortalScreen
import com.example.ui.auth.AuthGatewayScreen
import com.example.ui.chat.ChatListScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.VhoqxethDarkBg
import com.example.ui.theme.VhoqxethTheme
import kotlinx.coroutines.launch

enum class AppScreen {
    SPLASH,
    AUTH,
    CHAT_LIST,
    ADMIN_PORTAL
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VhoqxethTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VhoqxethDarkBg
                ) {
                    VhoqxethApp()
                }
            }
        }
    }
}

@Composable
fun VhoqxethApp() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }
    val ipManager = remember { DeviceIpManager(context) }

    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(sessionManager.getSavedUserProfile()) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            AppScreen.SPLASH -> {
                SplashScreen(
                    onSplashFinished = {
                        if (sessionManager.isLoggedIn()) {
                            // User is already logged in locally!
                            val cachedProfile = sessionManager.getSavedUserProfile()
                            currentUserProfile = cachedProfile
                            currentScreen = AppScreen.CHAT_LIST

                            // Non-blocking background IP synchronization for roaming networks
                            coroutineScope.launch {
                                try {
                                    val currentIp = ipManager.resolveDeviceIp()
                                    if (cachedProfile != null && currentIp.isNotBlank() && currentIp != cachedProfile.currentIp) {
                                        sessionManager.updateCurrentIp(currentIp)
                                        authRepository.linkIpToUserProfile(cachedProfile.customUserId, currentIp)
                                        Log.i("MainActivity", "Quietly updated roaming IP to $currentIp in background")
                                    }
                                } catch (e: Exception) {
                                    Log.w("MainActivity", "Background IP sync skipped: ${e.message}")
                                }
                            }
                        } else {
                            // First run or logged out: present login or signup gateway
                            currentScreen = AppScreen.AUTH
                        }
                    }
                )
            }
            AppScreen.AUTH -> {
                AuthGatewayScreen(
                    onAuthenticated = { profile ->
                        sessionManager.saveSession(profile)
                        currentUserProfile = profile
                        currentScreen = AppScreen.CHAT_LIST
                    },
                    onOpenAdminPortal = {
                        currentScreen = AppScreen.ADMIN_PORTAL
                    }
                )
            }
            AppScreen.CHAT_LIST -> {
                ChatListScreen(
                    userProfile = currentUserProfile,
                    onSignOut = {
                        sessionManager.clearSession()
                        currentUserProfile = null
                        currentScreen = AppScreen.AUTH
                    }
                )
            }
            AppScreen.ADMIN_PORTAL -> {
                AdminPortalScreen(
                    onExit = {
                        currentScreen = if (sessionManager.isLoggedIn()) {
                            AppScreen.CHAT_LIST
                        } else {
                            AppScreen.AUTH
                        }
                    }
                )
            }
        }
    }
}
