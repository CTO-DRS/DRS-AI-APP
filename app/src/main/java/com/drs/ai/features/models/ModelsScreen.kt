package com.drs.ai.features.models

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.core.models.CheckItem
import com.drs.ai.core.models.GgufParser
import com.drs.ai.core.models.HardwareProfiler
import com.drs.ai.core.models.ModelRepository
import com.drs.ai.data.db.LocalModel
import com.drs.ai.ui.components.EmptyState
import com.drs.ai.ui.components.GradientBanner
import com.drs.ai.ui.components.IconBadge
import com.drs.ai.ui.components.PrimaryAction
import com.drs.ai.ui.components.SectionCard
import com.drs.ai.ui.components.SoftAction
import com.drs.ai.ui.components.StatusBadge
import com.drs.ai.ui.components.entrance
import com.drs.ai.ui.components.pressScale
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
        progress = 0
        scope.launch {
            try {
                container.models.import(uri, kind) { p -> progress = p }
            } catch (e: ModelRepository.ImportException) {
                errorMsg = e.message
            } catch (e: Exception) {
                errorMsg = e.message
            } finally {
                importing = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GradientBanner(
            title = stringResource(R.string.models_title),
            subtitle = stringResource(R.string.models_import_hint)
        )

        // ── Import action + progress ─────────────────────────────────────
        Column(Modifier.entrance(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryAction(
                label = stringResource(R.string.models_import),
                icon = Icons.Filled.Add,
                onClick = { picker.launch(arrayOf("*/*")) }
            )
            if (importing) {
                SectionCard {
                    Text(stringResource(R.string.model_import_progress), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("$progress%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            errorMsg?.let {
                SectionCard {
                    Text(
                        stringResource(R.string.model_import_failed, it),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // ── Model list ───────────────────────────────────────────────────
        if (models.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Extension,
                title = stringResource(R.string.models_empty),
                hint = stringResource(R.string.models_empty_hint)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(models, key = { it.id }) { m ->
                    ModelCard(
                        model = m,
                        onSetActive = { scope.launch { container.models.setActive(m) } },
                        onDelete = {
                            scope.launch {
                                container.models.delete(m)
                                File(m.path).delete()
                            }
                        },
                        onCompat = { compatFor = m }
                    )
                }
                item { Spacer(Modifier.height(48.dp)) }
            }
        }
    }

    // ── Kind picker dialog ───────────────────────────────────────────────
    if (showKindPicker) {
        AlertDialog(
            onDismissRequest = { showKindPicker = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.models_import)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (k in ModelRepository.KINDS) {
                        val (icon, tint) = kindVisual(k)
                        Card(
                            onClick = {
                                showKindPicker = false
                                pendingUri?.let { importNow(it, k) }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconBadge(
                                    icon = icon,
                                    container = tint.copy(alpha = 0.16f),
                                    content = tint,
                                    size = 36.dp
                                )
                                Text(
                                    stringResource(
                                        when (k) {
                                            ModelRepository.KIND_CHAT -> R.string.model_kind_chat
                                            ModelRepository.KIND_EMBEDDER -> R.string.model_kind_embedder
                                            ModelRepository.KIND_MMPROJ -> R.string.model_kind_mmproj
                                            else -> R.string.model_kind_whisper
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showKindPicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // ── Compatibility dialog ─────────────────────────────────────────────
    compatFor?.let { model ->
        val report = remember(model) { container.models.compatReport(model, profile) }
        AlertDialog(
            onDismissRequest = { compatFor = null },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(model.name) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            when (report.overall) {
                                CheckItem.Status.FAIL -> Icons.Filled.Delete
                                CheckItem.Status.WARN -> Icons.Filled.Bolt
                                else -> Icons.Filled.Verified
                            },
                            null,
                            tint = when (report.overall) {
                                CheckItem.Status.FAIL -> MaterialTheme.colorScheme.error
                                CheckItem.Status.WARN -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
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
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Spacer(Modifier.height(10.dp))
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

private fun kindVisual(kind: String): Pair<ImageVector, androidx.compose.ui.graphics.Color> = when (kind) {
    ModelRepository.KIND_CHAT ->
        Icons.AutoMirrored.Filled.Chat to androidx.compose.ui.graphics.Color(0xFF5B4BD0)
    ModelRepository.KIND_EMBEDDER ->
        Icons.Filled.GraphicEq to androidx.compose.ui.graphics.Color(0xFF0E7490)
    ModelRepository.KIND_MMPROJ ->
        Icons.Filled.Image to androidx.compose.ui.graphics.Color(0xFF7C4DFF)
    else ->
        Icons.Filled.GraphicEq to androidx.compose.ui.graphics.Color(0xFFFFB74D)
}

@Composable
private fun ModelCard(
    model: LocalModel,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
    onCompat: () -> Unit
) {
    val (icon, tint) = kindVisual(model.kind)
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val kindLabel = stringResource(
        when (model.kind) {
            ModelRepository.KIND_CHAT -> R.string.model_kind_chat
            ModelRepository.KIND_EMBEDDER -> R.string.model_kind_embedder
            ModelRepository.KIND_MMPROJ -> R.string.model_kind_mmproj
            ModelRepository.KIND_WHISPER -> R.string.model_kind_whisper
            else -> R.string.model_kind_unknown
        }
    )

    Card(
        modifier = Modifier.fillMaxWidth().pressScale(interactionSource = interaction),
        onClick = onCompat,
        interactionSource = interaction,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = icon, container = tint.copy(alpha = 0.16f), content = tint)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        model.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                    Text(
                        kindLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (model.active) {
                    StatusBadge(stringResource(R.string.model_active), positive = true)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                listOfNotNull(
                    model.arch?.let { stringResource(R.string.model_arch, it) },
                    model.quant?.let { stringResource(R.string.model_quant, it) },
                    stringResource(R.string.model_size, formatSize(model.sizeBytes))
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            if (model.active) {
                SoftAction(
                    label = stringResource(R.string.compat_title),
                    icon = Icons.Filled.Verified,
                    onClick = onCompat
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryAction(
                        label = stringResource(
                            when (model.kind) {
                                ModelRepository.KIND_CHAT -> R.string.model_set_active
                                ModelRepository.KIND_EMBEDDER -> R.string.model_set_embedder
                                ModelRepository.KIND_MMPROJ -> R.string.model_set_mmproj
                                else -> R.string.model_set_whisper
                            }
                        ),
                        icon = Icons.Filled.Check,
                        onClick = onSetActive
                    )
                    SoftAction(label = stringResource(R.string.compat_title), onClick = onCompat)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun formatSize(b: Long): String = when {
    b >= 1L shl 30 -> "%.2f GB".format(b / 1073741824f)
    b >= 1L shl 20 -> "%.1f MB".format(b / 1048576f)
    else -> "%.0f KB".format(b / 1024f)
}
