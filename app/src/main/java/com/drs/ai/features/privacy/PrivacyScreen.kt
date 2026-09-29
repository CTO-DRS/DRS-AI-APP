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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.ui.components.SectionTitle
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleLarge)

        Card {
            Column(Modifier.padding(16.dp)) {
                SectionTitle(stringResource(R.string.privacy_offline_attestation))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.privacy_attestation_ok), style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (s != null) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (s!!.allowNetwork) stringResource(R.string.privacy_network_allowed)
                        else stringResource(R.string.privacy_network_blocked),
                        color = if (s!!.allowNetwork) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.privacy_toggle_network), Modifier.weight(1f))
                        Switch(
                            checked = s!!.allowNetwork,
                            onCheckedChange = { v -> scope.launch { container.settings.setAllowNetwork(v) } }
                        )
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.privacy_data_map))
        Text(stringResource(R.string.privacy_map_chats), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.privacy_map_files), style = MaterialTheme.typography.bodyMedium)

        SectionTitle(stringResource(R.string.privacy_contracts))
        Contract(stringResource(R.string.privacy_contract_chat), network = false)
        Contract(stringResource(R.string.privacy_contract_stt), network = false)
        Contract(stringResource(R.string.privacy_contract_tts), network = false)
        Contract(stringResource(R.string.privacy_contract_download), network = true)
        Contract(stringResource(R.string.privacy_crash_reports), network = false)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Contract(text: String, network: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            if (network) stringResource(R.string.privacy_network_allowed).substringBefore(' ') 
            else stringResource(R.string.badge_offline),
            style = MaterialTheme.typography.labelMedium,
            color = if (network) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
        )
    }
}
