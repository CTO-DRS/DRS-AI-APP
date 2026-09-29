package com.drs.ai.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.features.about.AboutScreen
import com.drs.ai.features.chat.ChatScreen
import com.drs.ai.features.dashboard.DashboardScreen
import com.drs.ai.features.documents.DocumentsScreen
import com.drs.ai.features.diagnostics.DiagnosticsScreen
import com.drs.ai.features.models.ModelsScreen
import com.drs.ai.features.privacy.PrivacyScreen
import com.drs.ai.features.settings.LockScreen
import com.drs.ai.features.settings.SettingsScreen
import com.drs.ai.features.tools.ToolsScreen
import com.drs.ai.features.vision.VisionScreen
import com.drs.ai.features.voice.VoiceScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

object Routes {
    const val DASHBOARD = "dashboard"
    const val CHAT = "chat"
    const val MODELS = "models"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val DOCUMENTS = "documents"
    const val VISION = "vision"
    const val VOICE = "voice"
    const val REMINDERS = "reminders"
    const val PRIVACY = "privacy"
    const val DIAGNOSTICS = "diagnostics"
    const val ABOUT = "about"
}

private class TabSpec(val route: String, val labelRes: Int, val icon: @Composable () -> Unit)

@Composable
fun DrsRoot() {
    val container = AppGraph.container
    val settings by container.settings.settings.collectAsState(initial = null)

    // App lock: stays locked until verified when a PIN exists
    var lockResolved by remember { mutableStateOf(false) }
    var unlocked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val s = container.settings.settings.first()
        unlocked = s.pinHash == null || s.pinSalt == null
        lockResolved = true
    }

    val s = settings
    if (lockResolved && s != null && s.pinHash != null && s.pinSalt != null && !unlocked) {
        LockScreen(
            storedHash = s.pinHash!!,
            storedSalt = s.pinSalt!!,
            onUnlocked = { unlocked = true }
        )
        return
    }

    // v1.4 — first-run onboarding wizard (once, skippable, after lock check)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    if (lockResolved && s != null && !s.onboardingDone) {
        com.drs.ai.features.onboarding.OnboardingScreen(
            onFinish = {
                unlocked = true
                scope.launch { container.settings.setOnboardingDone(true) }
            }
        )
        return
    }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val tabs = listOf(
        TabSpec(Routes.DASHBOARD, R.string.nav_dashboard) { Icon(Icons.Filled.Dashboard, null) },
        TabSpec(Routes.CHAT, R.string.nav_chat) { Icon(Icons.Filled.Chat, null) },
        TabSpec(Routes.MODELS, R.string.nav_models) { Icon(Icons.Filled.Extension, null) },
        TabSpec(Routes.TOOLS, R.string.nav_tools) { Icon(Icons.Filled.Handyman, null) },
        TabSpec(Routes.SETTINGS, R.string.nav_settings) { Icon(Icons.Filled.Settings, null) }
    )

    val showBar = currentRoute in tabs.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // v1.4: bottom bar slides in/out with the route — feels alive, saves space
            androidx.compose.animation.AnimatedVisibility(
                visible = showBar,
                enter = androidx.compose.animation.slideInVertically(
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.86f, stiffness = 340f
                    )
                ) { it } + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    for (tab in tabs) {
                        val selected = currentRoute == tab.route
                        val label = stringResource(tab.labelRes)
                        // v1.4: animated pill behind the icon + spring scale — modern morph
                        val pillColor by androidx.compose.animation.animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
                                          else Color.Transparent,
                            animationSpec = tween(240), label = "pill"
                        )
                        val iconScale by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (selected) 1.12f else 1f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.55f, stiffness = 420f
                            ), label = "tabScale"
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Box(
                                    Modifier
                                        .size(width = 44.dp, height = 30.dp)
                                        .background(pillColor, RoundedCornerShape(50))
                                ) {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    scaleX = iconScale
                                                    scaleY = iconScale
                                                },
                                            contentAlignment = Alignment.Center
                                        ) { tab.icon() }
                                    }
                                }
                            },
                            label = { Text(label, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // v1.4: RTL-safe screen transitions — soft fade + micro scale,
            // no directional slides (direction mirrors badly in Arabic).
            NavHost(
                navController = nav,
                startDestination = Routes.DASHBOARD,
                enterTransition = {
                    androidx.compose.animation.fadeIn(
                        androidx.compose.animation.core.tween(230)
                    ) + androidx.compose.animation.scaleIn(
                        initialScale = 0.965f,
                        animationSpec = androidx.compose.animation.core.tween(230)
                    )
                },
                exitTransition = {
                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(170))
                },
                popEnterTransition = {
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200))
                },
                popExitTransition = {
                    androidx.compose.animation.fadeOut(
                        androidx.compose.animation.core.tween(170)
                    ) + androidx.compose.animation.scaleOut(
                        targetScale = 0.975f,
                        animationSpec = androidx.compose.animation.core.tween(170)
                    )
                }
            ) {
                composable(Routes.DASHBOARD) { DashboardScreen(nav) }
                composable(Routes.CHAT) { ChatScreen(nav) }
                composable(Routes.MODELS) { ModelsScreen(nav) }
                composable(Routes.TOOLS) { ToolsScreen(nav) }
                composable(Routes.SETTINGS) { SettingsScreen(nav) }
                composable(Routes.DOCUMENTS) { DocumentsScreen(nav) }
                composable(Routes.VISION) { VisionScreen(nav) }
                composable(Routes.VOICE) { VoiceScreen(nav) }
                composable(Routes.REMINDERS) { com.drs.ai.features.reminders.RemindersScreen(nav) }
                composable(Routes.PRIVACY) { PrivacyScreen(nav) }
                composable(Routes.DIAGNOSTICS) { DiagnosticsScreen(nav) }
                composable(Routes.ABOUT) { AboutScreen(nav) }
            }
        }
    }
}
