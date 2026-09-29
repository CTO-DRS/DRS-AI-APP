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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.ui.components.SectionCard
import com.drs.ai.ui.components.SliderRow
import com.drs.ai.ui.components.SwitchRow
import com.drs.ai.ui.components.IconBadge
import com.drs.ai.ui.components.ScreenHeader
import com.drs.ai.ui.components.SoftAction
import com.drs.ai.ui.components.entrance
import com.drs.ai.ui.components.pressScale
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.about_tagline)
        )

        // ── Appearance ───────────────────────────────────────────────────
        SectionCard(Modifier.entrance(), title = stringResource(R.string.settings_appearance)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBadge(icon = Icons.Filled.Palette)
                Column {
                    Text(stringResource(R.string.settings_theme_mode), style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                        for ((mode, label) in listOf(0 to R.string.theme_system, 1 to R.string.theme_light, 2 to R.string.theme_dark)) {
                            FilterChip(
                                selected = s?.themeMode == mode,
                                onClick = { scope.launch { container.settings.setThemeMode(mode) } },
                                label = { Text(stringResource(label)) }
                            )
                        }
                    }
                }
            }
            if (s != null) {
                SwitchRow(
                    label = stringResource(R.string.settings_dynamic_colors),
                    checked = s!!.dynamicColors,
                    onChange = { v -> scope.launch { container.settings.setDynamicColors(v) } }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                for ((lang, label) in listOf("system" to R.string.lang_system, "en" to R.string.lang_en, "ar" to R.string.lang_ar)) {
                    FilterChip(
                        selected = s?.language == lang,
                        onClick = { scope.launch { container.settings.setLanguage(lang) } },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
        }

        // ── Engine ───────────────────────────────────────────────────────
        SectionCard(Modifier.entrance(), title = stringResource(R.string.settings_engine)) {
            IconBadge(
                icon = Icons.Filled.Memory,
                container = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                content = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(8.dp))
            SliderRow(
                label = stringResource(R.string.settings_threads),
                valueLabel = if (threads == 0) "auto" else threads.toString(),
                value = threads.toFloat(),
                range = 0f..8f,
                steps = 7,
                onChange = { threads = it.toInt(); scope.launch { container.settings.setThreads(threads) } }
            )
            SliderRow(
                label = stringResource(R.string.settings_context),
                valueLabel = "$ctx",
                value = ctx.toFloat(),
                range = 512f..16384f,
                onChange = { ctx = (it.toInt() / 256) * 256; scope.launch { container.settings.setContextSize(ctx) } }
            )
            SliderRow(
                label = stringResource(R.string.settings_batch),
                valueLabel = "$batch",
                value = batch.toFloat(),
                range = 64f..1024f,
                onChange = { batch = (it.toInt() / 64) * 64; scope.launch { container.settings.setBatchSize(batch) } }
            )
            if (s != null) {
                SwitchRow(
                    label = stringResource(R.string.settings_flash_attention),
                    checked = s!!.flashAttention,
                    onChange = { v -> scope.launch { container.settings.setFlashAttention(v) } }
                )
            }
            Text(
                stringResource(R.string.settings_gpu_layers_note),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Memory ───────────────────────────────────────────────────────
        SectionCard(Modifier.entrance(), title = stringResource(R.string.settings_memory_mgmt)) {
            SliderRow(
                label = stringResource(R.string.settings_max_ram_pct, ramPct),
                valueLabel = "$ramPct%",
                value = ramPct.toFloat(),
                range = 30f..90f,
                onChange = { ramPct = it.toInt(); scope.launch { container.settings.setMaxRamPct(ramPct) } }
            )
            if (s != null) {
                SwitchRow(
                    label = stringResource(R.string.settings_auto_unload),
                    checked = s!!.autoUnload,
                    onChange = { v -> scope.launch { container.settings.setAutoUnload(v) } }
                )
            }
        }

        // ── Security ─────────────────────────────────────────────────────
        SectionCard(Modifier.entrance(), title = stringResource(R.string.settings_security)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBadge(
                    icon = Icons.Filled.Lock,
                    container = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    content = MaterialTheme.colorScheme.secondary
                )
                if (s?.pinHash != null) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_app_lock_on),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(onClick = {
                            scope.launch { container.settings.setPin(null, null) }
                        }) { Text(stringResource(R.string.settings_app_lock_remove)) }
                    }
                } else {
                    Text(stringResource(R.string.settings_app_lock), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
            if (s?.pinHash == null) {
                SetPinSection()
            }
        }

        // ── Data ─────────────────────────────────────────────────────────
        SectionCard(Modifier.entrance(), title = stringResource(R.string.settings_data)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBadge(
                    icon = Icons.Filled.Storage,
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                }
            }
        }

        // ── Links ────────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LinkCard(
                icon = Icons.Filled.PrivacyTip,
                tint = MaterialTheme.colorScheme.tertiary,
                label = stringResource(R.string.privacy_title)
            ) { nav.navigate(Routes.PRIVACY) }
            LinkCard(
                icon = Icons.Filled.MonitorHeart,
                tint = MaterialTheme.colorScheme.secondary,
                label = stringResource(R.string.settings_diagnostics)
            ) { nav.navigate(Routes.DIAGNOSTICS) }
            LinkCard(
                icon = Icons.Filled.Info,
                tint = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.settings_about)
            ) { nav.navigate(Routes.ABOUT) }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun LinkCard(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, label: String, onClick: () -> Unit) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource = interaction),
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconBadge(
                icon = icon,
                container = tint.copy(alpha = 0.15f),
                content = tint,
                size = 38.dp
            )
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
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
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirm, onValueChange = { confirm = it.filter { c -> c.isDigit() }.take(8) },
            label = { Text(stringResource(R.string.lock_confirm_pin)) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        if (mismatch) Text(stringResource(R.string.lock_mismatch), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        Text(
            stringResource(R.string.lock_use_biometrics_note),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        SoftAction(
            label = stringResource(R.string.settings_app_lock_set),
            icon = Icons.Filled.Lock,
            onClick = {
                if (pin.length >= 4 && pin == confirm) {
                    mismatch = false
                    val salt = PinCrypto.newSalt()
                    scope.launch {
                        container.settings.setPin(PinCrypto.hash(pin, salt), salt)
                    }
                    pin = ""; confirm = ""
                } else mismatch = true
            },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
