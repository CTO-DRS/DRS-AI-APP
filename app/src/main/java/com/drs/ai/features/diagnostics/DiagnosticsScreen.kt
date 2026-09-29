package com.drs.ai.features.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.core.diagnostics.DiagnosticsCollector
import com.drs.ai.ui.components.InfoRow
import com.drs.ai.ui.components.PrimaryAction
import com.drs.ai.ui.components.ScreenHeader
import com.drs.ai.ui.components.SectionCard
import com.drs.ai.ui.components.SoftAction
import com.drs.ai.ui.components.entrance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DiagnosticsScreen(nav: NavController) {
    val container = AppGraph.container
    val context = LocalContext.current
    var report by remember { mutableStateOf<DiagnosticsCollector.Report?>(null) }
    var exported by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    suspend fun refresh() {
        val s = container.settings.settings.first()
        report = container.diagnostics.collect(s.threads.takeIf { it > 0 } ?: 2)
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.diag_title),
            subtitle = stringResource(R.string.dashboard_subtitle)
        )

        report?.let { r ->
            SectionCard(Modifier.entrance(), title = stringResource(R.string.diag_device)) {
                InfoRow("Device", r.device)
                InfoRow(stringResource(R.string.diag_device), r.androidVersion)
                InfoRow(stringResource(R.string.dashboard_abi), r.abi)
                InfoRow(stringResource(R.string.dashboard_cores), r.cores.toString())
                InfoRow(stringResource(R.string.diag_ram_total), "%.1f GB".format(r.ramTotalGB))
                InfoRow(stringResource(R.string.diag_ram_avail), "%.1f GB".format(r.ramAvailGB))
                InfoRow(stringResource(R.string.diag_low_ram), if (r.lowRamDevice) stringResource(R.string.yes) else stringResource(R.string.no))
                InfoRow(stringResource(R.string.diag_storage), "%.1f GB".format(r.storageFreeGB))
                InfoRow(stringResource(R.string.diag_thermal), r.thermal)
                InfoRow(stringResource(R.string.diag_gpu), r.vulkan)
                InfoRow(stringResource(R.string.diag_profile), r.profile + " (max ctx " + r.maxContext + ")")
            }
            SectionCard(title = stringResource(R.string.diag_engine)) {
                InfoRow(stringResource(R.string.diag_engine_loaded), if (r.chatEngineLoaded) "${r.chatModel} (ctx used ${r.chatCtxUsed})" else stringResource(R.string.diag_engine_none))
                InfoRow(stringResource(R.string.model_kind_embedder), if (r.embedderLoaded) stringResource(R.string.yes) else stringResource(R.string.diag_engine_none))
                InfoRow(stringResource(R.string.diag_threads), r.threads.toString())
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryAction(
                label = stringResource(R.string.diag_refresh),
                icon = Icons.Filled.Refresh,
                onClick = { scope.launch { refresh() } }
            )
            SoftAction(
                label = stringResource(R.string.diag_export),
                icon = Icons.Filled.Save,
                onClick = {
                    scope.launch {
                        refresh()
                        val s = container.settings.settings.first()
                        val r = container.diagnostics.collect(s.threads.takeIf { it > 0 } ?: 2)
                        val dir = File(context.filesDir, "diagnostics").apply { mkdirs() }
                        val f = File(dir, "drs_diagnostics_${System.currentTimeMillis()}.json")
                        val json = buildString {
                            append("{\n")
                            append("  \"device\": \"${r.device}\",\n")
                            append("  \"android\": \"${r.androidVersion}\",\n")
                            append("  \"abi\": \"${r.abi}\",\n")
                            append("  \"cores\": ${r.cores},\n")
                            append("  \"ram_total_gb\": ${"%.2f".format(r.ramTotalGB)},\n")
                            append("  \"ram_avail_gb\": ${"%.2f".format(r.ramAvailGB)},\n")
                            append("  \"low_ram\": ${r.lowRamDevice},\n")
                            append("  \"storage_free_gb\": ${"%.2f".format(r.storageFreeGB)},\n")
                            append("  \"thermal\": \"${r.thermal}\",\n")
                            append("  \"vulkan\": \"${r.vulkan}\",\n")
                            append("  \"profile\": \"${r.profile}\",\n")
                            append("  \"chat_loaded\": ${r.chatEngineLoaded},\n")
                            append("  \"chat_model\": ${r.chatModel?.let { "\"$it\"" } ?: "null"},\n")
                            append("  \"ctx_used\": ${r.chatCtxUsed},\n")
                            append("  \"embedder_loaded\": ${r.embedderLoaded},\n")
                            append("  \"threads\": ${r.threads}\n")
                            append("}\n")
                        }
                        f.writeText(json)
                        exported = f.absolutePath
                    }
                }
            )
        }
        exported?.let {
            SectionCard {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}
