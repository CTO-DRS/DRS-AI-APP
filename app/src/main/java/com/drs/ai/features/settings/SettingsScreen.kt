package com.drs.ai.features.settings

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.ui.components.SectionTitle
import com.drs.ai.ui.nav.Routes
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(nav: NavController) {
    val container = AppGraph.container
    val s by container.settings.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var threads by remember { mutableIntStateOf(0) }
    var ctx by remember { mutableIntStateOf(2048) }
    var batch by remember { mutableIntStateOf(256) }
    var ramPct by remember { mutableIntStateOf(60) }

    LaunchedEffect(s?.threads) {
        val cur = s ?: return@LaunchedEffect
        threads = cur.threads
        ctx = cur.contextSize
        batch = cur.batchSize
        ramPct = cur.maxRamPct
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)

        // Appearance
        SectionTitle(stringResource(R.string.settings_appearance))
        if (s != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((mode, label) in listOf(0 to R.string.theme_system, 1 to R.string.theme_light, 2 to R.string.theme_dark)) {
                    FilterChip(
                        selected = s!!.themeMode == mode,
                        onClick = { scope.launch { container.settings.setThemeMode(mode) } },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.settings_dynamic_colors))
                Switch(checked = s!!.dynamicColors, onCheckedChange = { v -> scope.launch { container.settings.setDynamicColors(v) } })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((lang, label) in listOf("system" to R.string.lang_system, "en" to R.string.lang_en, "ar" to R.string.lang_ar)) {
                    FilterChip(
                        selected = s!!.language == lang,
                        onClick = { scope.launch { container.settings.setLanguage(lang) } },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
        }

        // Engine
        SectionTitle(stringResource(R.string.settings_engine))
        Text(stringResource(R.string.settings_threads) + " — " + (if (threads == 0) "auto" else threads.toString()))
        Slider(threads.toFloat(), { threads = it.toInt(); scope.launch { container.settings.setThreads(threads) } }, valueRange = 0f..8f, steps = 7)
        Text(stringResource(R.string.settings_context) + " — $ctx")
        Slider(ctx.toFloat(), { ctx = (it.toInt() / 256) * 256; scope.launch { container.settings.setContextSize(ctx) } }, valueRange = 512f..16384f)
        Text(stringResource(R.string.settings_batch) + " — $batch")
        Slider(batch.toFloat(), { batch = (it.toInt() / 64) * 64; scope.launch { container.settings.setBatchSize(batch) } }, valueRange = 64f..1024f)
        if (s != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.settings_flash_attention))
                Switch(checked = s!!.flashAttention, onCheckedChange = { v -> scope.launch { container.settings.setFlashAttention(v) } })
            }
        }
        Text(stringResource(R.string.settings_gpu_layers_note), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Resources
        SectionTitle(stringResource(R.string.settings_memory_mgmt))
        Text(stringResource(R.string.settings_max_ram_pct, ramPct))
        Slider(ramPct.toFloat(), { ramPct = it.toInt(); scope.launch { container.settings.setMaxRamPct(ramPct) } }, valueRange = 30f..90f)
        if (s != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.settings_auto_unload))
                Switch(checked = s!!.autoUnload, onCheckedChange = { v -> scope.launch { container.settings.setAutoUnload(v) } })
            }
        }

        // Security
        SectionTitle(stringResource(R.string.settings_security))
        if (s?.pinHash != null) {
            Text(stringResource(R.string.settings_app_lock_on), color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = {
                scope.launch { container.settings.setPin(null, null) }
            }) { Text(stringResource(R.string.settings_app_lock_remove)) }
        } else {
            SetPinSection()
        }

        // Data
        SectionTitle(stringResource(R.string.settings_data))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { scope.launch { container.db.chatDao().clearAllMessages(); container.db.chatDao().clearAllSessions() } }) {
                Text(stringResource(R.string.settings_clear_chats))
            }
            TextButton(onClick = { scope.launch { container.memory.clearAll() } }) {
                Text(stringResource(R.string.settings_clear_memory))
            }
        }
        TextButton(onClick = { scope.launch { container.rag.clearAll() } }) {
            Text(stringResource(R.string.settings_clear_docs))
        }

        // Links
        SectionTitle(stringResource(R.string.settings_privacy))
        TextButton(onClick = { nav.navigate(Routes.PRIVACY) }) { Text(stringResource(R.string.privacy_title)) }
        TextButton(onClick = { nav.navigate(Routes.DIAGNOSTICS) }) { Text(stringResource(R.string.settings_diagnostics)) }
        TextButton(onClick = { nav.navigate(Routes.ABOUT) }) { Text(stringResource(R.string.settings_about)) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SetPinSection() {
    val container = AppGraph.container
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = pin, onValueChange = { pin = it.filter { c -> c.isDigit() }.take(8) },
            label = { Text(stringResource(R.string.lock_set_pin)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirm, onValueChange = { confirm = it.filter { c -> c.isDigit() }.take(8) },
            label = { Text(stringResource(R.string.lock_confirm_pin)) },
            modifier = Modifier.fillMaxWidth()
        )
        if (mismatch) Text(stringResource(R.string.lock_mismatch), color = MaterialTheme.colorScheme.error)
        Text(stringResource(R.string.lock_use_biometrics_note), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = {
            if (pin.length >= 4 && pin == confirm) {
                mismatch = false
                val salt = PinCrypto.newSalt()
                scope.launch {
                    container.settings.setPin(PinCrypto.hash(pin, salt), salt)
                }
                pin = ""; confirm = ""
            } else mismatch = true
        }) { Text(stringResource(R.string.settings_app_lock_set)) }
    }
}
