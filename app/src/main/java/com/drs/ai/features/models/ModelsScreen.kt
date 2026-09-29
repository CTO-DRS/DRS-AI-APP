package com.drs.ai.features.models

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.drs.ai.core.models.CheckItem
import com.drs.ai.core.models.GgufParser
import com.drs.ai.core.models.HardwareProfiler
import com.drs.ai.core.models.ModelRepository
import com.drs.ai.data.db.LocalModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ModelsScreen(nav: NavController) {
    val container = AppGraph.container
    val scope = rememberCoroutineScope()
    val models by container.models.observeModels().collectAsState(initial = emptyList())

    var importing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var compatFor by remember { mutableStateOf<LocalModel?>(null) }
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingKind by remember { mutableStateOf(ModelRepository.KIND_CHAT) }
    var showKindPicker by remember { mutableStateOf(false) }

    val profile = remember { HardwareProfiler.probe(container.appContext) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingUri = uri
            showKindPicker = true
        }
    }

    fun importNow(uri: android.net.Uri, kind: String) {
        importing = true
        errorMsg = null
        successMsg = null
        progress = 0
        scope.launch {
            try {
                container.models.import(uri, kind) { p -> progress = p }
                successMsg = null
                // signal success via errorMsg=null + importing=false
            } catch (e: ModelRepository.ImportException) {
                errorMsg = e.message
            } catch (e: Exception) {
                errorMsg = e.message
            } finally {
                importing = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.models_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.models_import_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (importing) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.model_import_progress))
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        }
        errorMsg?.let {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.model_import_failed, it), color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker.launch(arrayOf("*/*")) }) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.height(0.dp))
                Text(" " + stringResource(R.string.models_import))
            }
        }

        Spacer(Modifier.height(16.dp))
        if (models.isEmpty()) {
            Text(stringResource(R.string.models_empty), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.models_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(models, key = { it.id }) { m ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(m.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2)
                                IconButton(onClick = {
                                    scope.launch {
                                        container.models.delete(m)
                                        File(m.path).delete()
                                    }
                                }) { Icon(Icons.Filled.Delete, null) }
                            }
                            val kindLabel = stringResource(
                                when (m.kind) {
                                    ModelRepository.KIND_CHAT -> R.string.model_kind_chat
                                    ModelRepository.KIND_EMBEDDER -> R.string.model_kind_embedder
                                    ModelRepository.KIND_MMPROJ -> R.string.model_kind_mmproj
                                    ModelRepository.KIND_WHISPER -> R.string.model_kind_whisper
                                    else -> R.string.model_kind_unknown
                                }
                            )
                            Text(kindLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                listOfNotNull(
                                    m.arch?.let { stringResource(R.string.model_arch, it) },
                                    m.quant?.let { stringResource(R.string.model_quant, it) },
                                    stringResource(R.string.model_size, formatSize(m.sizeBytes))
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (m.active) {
                                Text(
                                    stringResource(
                                        when (m.kind) {
                                            ModelRepository.KIND_CHAT -> R.string.model_active
                                            ModelRepository.KIND_EMBEDDER -> R.string.model_active_embedder
                                            ModelRepository.KIND_MMPROJ -> R.string.model_active_mmproj
                                            else -> R.string.model_active_whisper
                                        }
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { scope.launch { container.models.setActive(m) } }) {
                                        Text(
                                            stringResource(
                                                when (m.kind) {
                                                    ModelRepository.KIND_CHAT -> R.string.model_set_active
                                                    ModelRepository.KIND_EMBEDDER -> R.string.model_set_embedder
                                                    ModelRepository.KIND_MMPROJ -> R.string.model_set_mmproj
                                                    else -> R.string.model_set_whisper
                                                }
                                            )
                                        )
                                    }
                                }
                            }
                            TextButton(onClick = { compatFor = m }) {
                                Text(stringResource(R.string.compat_title))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showKindPicker) {
        AlertDialog(
            onDismissRequest = { showKindPicker = false },
            title = { Text(stringResource(R.string.models_import)) },
            text = {
                Column {
                    for (k in ModelRepository.KINDS) {
                        TextButton(onClick = {
                            showKindPicker = false
                            pendingUri?.let { importNow(it, k) }
                        }) {
                            Text(
                                stringResource(
                                    when (k) {
                                        ModelRepository.KIND_CHAT -> R.string.model_kind_chat
                                        ModelRepository.KIND_EMBEDDER -> R.string.model_kind_embedder
                                        ModelRepository.KIND_MMPROJ -> R.string.model_kind_mmproj
                                        else -> R.string.model_kind_whisper
                                    }
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showKindPicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    compatFor?.let { model ->
        val report = remember(model) { container.models.compatReport(model, profile) }
        AlertDialog(
            onDismissRequest = { compatFor = null },
            title = { Text(model.name) },
            text = {
                Column {
                    Text(
                        stringResource(
                            when (report.overall) {
                                CheckItem.Status.PASS -> R.string.compat_pass
                                CheckItem.Status.WARN -> R.string.compat_warn
                                CheckItem.Status.FAIL -> R.string.compat_fail
                            }
                        ),
                        color = when (report.overall) {
                            CheckItem.Status.FAIL -> MaterialTheme.colorScheme.error
                            CheckItem.Status.WARN -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    for (item in report.items) {
                        Text("• ${item.name}: ${item.note}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { compatFor = null }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

private fun formatSize(b: Long): String = when {
    b >= 1L shl 30 -> "%.2f GB".format(b / 1073741824f)
    b >= 1L shl 20 -> "%.1f MB".format(b / 1048576f)
    else -> "%.0f KB".format(b / 1024f)
}
