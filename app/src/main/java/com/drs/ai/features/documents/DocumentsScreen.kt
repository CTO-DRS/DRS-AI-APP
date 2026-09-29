package com.drs.ai.features.documents

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.core.rag.RagPipeline
import com.drs.ai.ui.components.EmptyState
import com.drs.ai.ui.components.GradientBanner
import com.drs.ai.ui.components.IconBadge
import com.drs.ai.ui.components.PrimaryAction
import com.drs.ai.ui.components.SectionCard
import com.drs.ai.ui.components.StatusBadge
import com.drs.ai.ui.components.entrance
import com.drs.ai.ui.components.pressScale
import kotlinx.coroutines.launch

@Composable
fun DocumentsScreen(nav: NavController) {
    val container = AppGraph.container
    val scope = rememberCoroutineScope()
    val docs by container.db.ragDao().observeDocuments().collectAsState(initial = emptyList())

    var busy by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var importInfo by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<RagPipeline.SearchHit>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }

    val docNames = remember(docs) { docs.associate { it.id to it.name } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            errorMsg = null
            importInfo = null
            scope.launch {
                try {
                    val name = queryName(container.appContext, uri) ?: "document"
                    val chunks = container.rag.importDocument(uri, name, null) { p -> progressText = p }
                    importInfo = chunks.toString()
                } catch (e: Exception) {
                    errorMsg = e.message ?: "import failed"
                } finally {
                    busy = false
                }
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
            title = stringResource(R.string.docs_title),
            subtitle = stringResource(R.string.docs_supported)
        )

        // ── Import + status ──────────────────────────────────────────────
        Column(Modifier.entrance(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryAction(
                label = stringResource(R.string.docs_import),
                icon = Icons.Filled.Description,
                onClick = { picker.launch(arrayOf("*/*")) }
            )
            if (busy) {
                SectionCard {
                    Text(
                        progressText.ifEmpty { stringResource(R.string.docs_import_progress) },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        Modifier.fillMaxWidth().height(6.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            errorMsg?.let {
                SectionCard {
                    Text(
                        stringResource(R.string.docs_import_failed, it),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            importInfo?.let {
                SectionCard {
                    Text(
                        stringResource(R.string.docs_import_success, it),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // ── Semantic search ──────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.docs_search_hint)) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    scope.launch {
                        results = container.rag.search(query, 5)
                        searched = true
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp))
            ) {
                Icon(
                    Icons.Filled.Search, null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        if (searched) {
            if (results.isEmpty()) {
                Text(
                    stringResource(R.string.docs_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(R.string.docs_results, results.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                for (hit in results) {
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .pressScale(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() })
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "${docNames[hit.docId] ?: "?"} #${hit.chunkIndex}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                StatusBadge(
                                    stringResource(R.string.docs_score, hit.score),
                                    positive = true
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(hit.text, style = MaterialTheme.typography.bodyMedium, maxLines = 6)
                        }
                    }
                }
            }
        }

        // ── Library ──────────────────────────────────────────────────────
        if (docs.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Description,
                title = stringResource(R.string.docs_title),
                hint = stringResource(R.string.docs_empty)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(docs, key = { it.id }) { d ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(
                                icon = Icons.Filled.Description,
                                container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                content = MaterialTheme.colorScheme.primary,
                                size = 40.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(d.name, maxLines = 2, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                Text(
                                    stringResource(R.string.docs_chunks, d.chunks),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { scope.launch { container.rag.delete(d.id) } }) {
                                Icon(
                                    Icons.Filled.Delete, null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(48.dp)) }
            }
        }
    }
}

private fun queryName(context: android.content.Context, uri: android.net.Uri): String? =
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
    }
