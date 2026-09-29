package com.drs.ai.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.core.models.DeviceProfile
import com.drs.ai.core.models.HardwareProfiler
import com.drs.ai.ui.components.InfoRow
import com.drs.ai.ui.components.SectionTitle
import com.drs.ai.ui.components.StatusBadge
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
    val vulkanText = profile.vulkanVersion?.let { stringResource(R.string.diag_vulkan_ok, it) }
        ?: stringResource(R.string.diag_vulkan_no)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(stringResource(R.string.badge_local), positive = true)
            StatusBadge(stringResource(R.string.badge_offline), positive = true)
            if (activeChat == null) StatusBadge(stringResource(R.string.badge_no_model), positive = false)
        }

        Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.dashboard_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SectionTitle(stringResource(R.string.dashboard_model_status))
                Spacer(Modifier.height(8.dp))
                val modelName = activeChat?.name
                if (modelName == null) {
                    Text(stringResource(R.string.dashboard_no_model_installed), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(stringResource(R.string.dashboard_active_model, modelName), style = MaterialTheme.typography.bodyMedium)
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

        Card(Modifier.fillMaxWidth()) {
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

        SectionTitle(stringResource(R.string.dashboard_quick_actions))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionCard(Modifier.weight(1f), stringResource(R.string.action_start_chat), Icons.Filled.Chat) {
                nav.navigate(Routes.CHAT)
            }
            ActionCard(Modifier.weight(1f), stringResource(R.string.action_add_model), Icons.Filled.Add) {
                nav.navigate(Routes.MODELS)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionCard(Modifier.weight(1f), stringResource(R.string.action_add_docs), Icons.Filled.Description) {
                nav.navigate(Routes.DOCUMENTS)
            }
            ActionCard(Modifier.weight(1f), stringResource(R.string.action_diagnose), Icons.Filled.MonitorHeart) {
                nav.navigate(Routes.DIAGNOSTICS)
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun ActionCard(modifier: Modifier, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(modifier = modifier, onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
