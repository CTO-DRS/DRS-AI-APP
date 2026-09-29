package com.drs.ai.features.documents

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.core.rag.RagPipeline
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.docs_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.docs_supported), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        if (busy) {
            Text(progressText.ifEmpty { stringResource(R.string.docs_import_progress) })
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        errorMsg?.let { Text(stringResource(R.string.docs_import_failed, it), color = MaterialTheme.colorScheme.error) }
        importInfo?.let { Text(stringResource(R.string.docs_import_success, it), color = MaterialTheme.colorScheme.primary) }

        Button(onClick = { picker.launch(arrayOf("*/*")) }) {
            Text(stringResource(R.string.docs_import))
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.docs_search_hint)) },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                scope.launch {
                    results = container.rag.search(query, 5)
                    searched = true
                }
            }) { Icon(Icons.Filled.Search, null) }
        }
        if (searched) {
            if (results.isEmpty()) {
                Text(stringResource(R.string.docs_no_results), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(stringResource(R.string.docs_results, results.size), style = MaterialTheme.typography.titleMedium)
                for (hit in results) {
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "${docNames[hit.docId] ?: "?"} #${hit.chunkIndex} — " + stringResource(R.string.docs_score, hit.score),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(hit.text, style = MaterialTheme.typography.bodyMedium, maxLines = 6)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        if (docs.isEmpty()) {
            Text(stringResource(R.string.docs_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(docs, key = { it.id }) { d ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(d.name, maxLines = 2)
                                Text(
                                    stringResource(R.string.docs_chunks, d.chunks),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { scope.launch { container.rag.delete(d.id) } }) {
                                Icon(Icons.Filled.Delete, null)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun queryName(context: android.content.Context, uri: android.net.Uri): String? =
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
    }
