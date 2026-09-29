package com.drs.ai.features.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.BuildConfig
import com.drs.ai.R
import com.drs.ai.ui.components.SectionTitle

@Composable
fun AboutScreen(nav: NavController) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.about_tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyMedium)

        SectionTitle(stringResource(R.string.about_third_party))
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("llama.cpp (b6000) — MIT — ggml.ai & contributors", style = MaterialTheme.typography.bodyMedium)
                Text("whisper.cpp (v1.7.4) — MIT — Georgi Gerganov & contributors", style = MaterialTheme.typography.bodyMedium)
                Text("GGML — MIT — Georgi Gerganov & contributors", style = MaterialTheme.typography.bodyMedium)
                Text("AndroidX / Jetpack Compose / Room / DataStore — Apache-2.0", style = MaterialTheme.typography.bodyMedium)
                Text("kotlinx.serialization — Apache-2.0", style = MaterialTheme.typography.bodyMedium)
                Text("PDFBox-Android (Tom Roush) — Apache-2.0", style = MaterialTheme.typography.bodyMedium)
                Text("jsoup — MIT — Jonathan Hedley", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(stringResource(R.string.about_license), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
    }
}
