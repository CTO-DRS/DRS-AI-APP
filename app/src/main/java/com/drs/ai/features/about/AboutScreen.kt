package com.drs.ai.features.about

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.BuildConfig
import com.drs.ai.R
import com.drs.ai.ui.components.SectionCard
import com.drs.ai.ui.components.StatusBadge
import com.drs.ai.ui.components.heroGradient
import com.drs.ai.ui.components.entrance

@Composable
fun AboutScreen(nav: NavController) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Brand hero ───────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .background(brush = heroGradient(), shape = RoundedCornerShape(24.dp))
        ) {
            Column(Modifier.padding(20.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.SmartToy, null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.labelMedium,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.about_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                )
            }
        }

        SectionCard(Modifier.entrance()) {
            Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(stringResource(R.string.badge_local), positive = true)
                StatusBadge(stringResource(R.string.badge_offline), positive = true)
            }
        }

        SectionCard(title = stringResource(R.string.about_third_party)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("llama.cpp (b6000) — MIT — ggml.ai & contributors", style = MaterialTheme.typography.bodyMedium)
                Text("whisper.cpp (v1.7.4) — MIT — Georgi Gerganov & contributors", style = MaterialTheme.typography.bodyMedium)
                Text("GGML — MIT — Georgi Gerganov & contributors", style = MaterialTheme.typography.bodyMedium)
                Text("AndroidX / Jetpack Compose / Room / DataStore — Apache-2.0", style = MaterialTheme.typography.bodyMedium)
                Text("kotlinx.serialization — Apache-2.0", style = MaterialTheme.typography.bodyMedium)
                Text("PDFBox-Android (Tom Roush) — Apache-2.0", style = MaterialTheme.typography.bodyMedium)
                Text("jsoup — MIT — Jonathan Hedley", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(
            stringResource(R.string.about_license),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))
    }
}
