package com.drs.ai.features.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.ui.components.IconBadge
import com.drs.ai.ui.components.ScreenHeader
import com.drs.ai.ui.components.SectionCard
import com.drs.ai.ui.components.StatusBadge
import com.drs.ai.ui.components.SwitchRow
import com.drs.ai.ui.components.entrance
import kotlinx.coroutines.launch

@Composable
fun PrivacyScreen(nav: NavController) {
    val container = AppGraph.container
    val s by container.settings.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.privacy_title),
            subtitle = stringResource(R.string.about_tagline)
        )

        // ── Offline attestation ──────────────────────────────────────────
        SectionCard(Modifier.entrance()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBadge(
                    icon = Icons.Filled.CloudOff,
                    container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    content = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.privacy_offline_attestation),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.privacy_attestation_ok), style = MaterialTheme.typography.bodyMedium)
        }

        if (s != null) {
            SectionCard(title = null) {
                Text(
                    if (s!!.allowNetwork) stringResource(R.string.privacy_network_allowed)
                    else stringResource(R.string.privacy_network_blocked),
                    color = if (s!!.allowNetwork) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                SwitchRow(
                    label = stringResource(R.string.privacy_toggle_network),
                    checked = s!!.allowNetwork,
                    onChange = { v -> scope.launch { container.settings.setAllowNetwork(v) } }
                )
            }
        }

        SectionCard(title = stringResource(R.string.privacy_data_map)) {
            Text(stringResource(R.string.privacy_map_chats), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.privacy_map_files), style = MaterialTheme.typography.bodyMedium)
        }

        SectionCard(title = stringResource(R.string.privacy_contracts)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Contract(stringResource(R.string.privacy_contract_chat), network = false)
                Contract(stringResource(R.string.privacy_contract_stt), network = false)
                Contract(stringResource(R.string.privacy_contract_tts), network = false)
                Contract(stringResource(R.string.privacy_contract_download), network = true)
                Contract(stringResource(R.string.privacy_crash_reports), network = false)
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun Contract(text: String, network: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        StatusBadge(
            if (network) stringResource(R.string.privacy_network_allowed).substringBefore(' ')
            else stringResource(R.string.badge_offline),
            positive = !network
        )
    }
}
