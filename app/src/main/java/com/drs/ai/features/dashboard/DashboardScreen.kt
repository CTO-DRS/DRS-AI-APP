package com.drs.ai.features.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.core.models.DeviceProfile
import com.drs.ai.core.models.HardwareProfiler
import com.drs.ai.ui.components.InfoRow
import com.drs.ai.ui.components.PulsingDot
import com.drs.ai.ui.components.SectionTitle
import com.drs.ai.ui.components.StatusBadge
import com.drs.ai.ui.components.heroGradient
import com.drs.ai.ui.components.pressScale
import com.drs.ai.ui.nav.Routes

@Composable
fun DashboardScreen(nav: NavController) {
    val container = AppGraph.container
    val context = androidx.compose.ui.platform.LocalContext.current
    val profile = remember { HardwareProfiler.probe(context) }

    val activeChat by container.models.observeActiveChat().collectAsState(initial = null)
    val chatState by container.engines.chat.state.collectAsState()

    val profileName = stringResource(
        when (profile.level) {
            DeviceProfile.ENTRY -> R.string.profile_entry
            DeviceProfile.BALANCED -> R.string.profile_balanced
            DeviceProfile.PERFORMANCE -> R.string.profile_performance
            DeviceProfile.EXTREME -> R.string.profile_extreme
        }
    )

    // one-shot entrance fade for the hero
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val heroAlpha by animateFloatAsState(if (shown) 1f else 0f, tween(450), label = "heroAlpha")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Hero ─────────────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .alpha(heroAlpha)
                .background(brush = heroGradient(), shape = RoundedCornerShape(26.dp))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(stringResource(R.string.badge_local), positive = true)
                    StatusBadge(stringResource(R.string.badge_offline), positive = true)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.dashboard_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.dashboard_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // ── Model status ─────────────────────────────────────────────────────
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(stringResource(R.string.dashboard_model_status))
                    Spacer(Modifier.weight(1f))
                    val generating = chatState is com.drs.ai.core.ai.EngineState.Generating
                    if (generating) {
                        PulsingDot(MaterialTheme.colorScheme.secondary)
                    } else if (activeChat != null) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                val modelName = activeChat?.name
                if (modelName == null) {
                    Text(stringResource(R.string.dashboard_no_model_installed), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        stringResource(R.string.dashboard_active_model, modelName),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    val stateText = when (val st = chatState) {
                        is com.drs.ai.core.ai.EngineState.Ready -> stringResource(R.string.engine_state_idle)
                        is com.drs.ai.core.ai.EngineState.Loading -> stringResource(R.string.engine_state_loading)
                        is com.drs.ai.core.ai.EngineState.Generating -> stringResource(R.string.engine_state_generating)
                        is com.drs.ai.core.ai.EngineState.Error -> stringResource(R.string.engine_state_error) + " — " + st.message
                        else -> stringResource(R.string.engine_state_idle)
                    }
                    Text(stateText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── Device profile ───────────────────────────────────────────────────
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                SectionTitle(stringResource(R.string.dashboard_device_profile) + " — " + profileName)
                Spacer(Modifier.height(8.dp))
                InfoRow(stringResource(R.string.dashboard_memory), "%.1f / %.1f GB".format(profile.ramAvailGB, profile.ramTotalGB))
                InfoRow(stringResource(R.string.dashboard_cores), stringResource(R.string.cores_fmt, profile.cores))
                InfoRow(stringResource(R.string.dashboard_abi), profile.abi)
                InfoRow(stringResource(R.string.dashboard_android), profile.androidVersion)
                InfoRow(stringResource(R.string.dashboard_vulkan), if (profile.vulkanVersion != null) "1.1+" else stringResource(R.string.none))
                InfoRow(stringResource(R.string.dashboard_storage_free), "%.1f GB".format(profile.storageFreeGB))
            }
        }

        // ── Quick actions ────────────────────────────────────────────────────
        SectionTitle(stringResource(R.string.dashboard_quick_actions))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Modifier.weight(1f), stringResource(R.string.action_start_chat), Icons.AutoMirrored.Filled.Chat) {
                nav.navigate(Routes.CHAT)
            }
            QuickAction(Modifier.weight(1f), stringResource(R.string.action_add_model), Icons.Filled.Add) {
                nav.navigate(Routes.MODELS)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Modifier.weight(1f), stringResource(R.string.action_add_docs), Icons.Filled.Description) {
                nav.navigate(Routes.DOCUMENTS)
            }
            QuickAction(Modifier.weight(1f), stringResource(R.string.action_diagnose), Icons.Filled.MonitorHeart) {
                nav.navigate(Routes.DIAGNOSTICS)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Modifier.weight(1f), stringResource(R.string.nav_vision), Icons.Filled.Image) {
                nav.navigate(Routes.VISION)
            }
            QuickAction(Modifier.weight(1f), stringResource(R.string.nav_voice), Icons.Filled.Mic) {
                nav.navigate(Routes.VOICE)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Modifier.weight(1f), stringResource(R.string.nav_reminders), Icons.Filled.Alarm) {
                nav.navigate(Routes.REMINDERS)
            }
            QuickAction(Modifier.weight(1f), stringResource(R.string.nav_settings), Icons.Filled.Settings) {
                nav.navigate(Routes.SETTINGS)
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .pressScale(interactionSource = interaction),
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 2)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
