package com.drs.ai.features.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.data.db.ChatMessage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(nav: NavController, vm: ChatViewModel = viewModel(factory = ChatViewModel.factory())) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val messages by vm.messages.collectAsState()
    val session by vm.session.collectAsState()
    val generating by vm.generating.collectAsState()
    val streamText by vm.streamText.collectAsState()
    val error by vm.error.collectAsState()
    val notice by vm.notice.collectAsState()
    val lastStats by vm.lastStats.collectAsState()
    val ctxUsed by vm.ctxUsed.collectAsState()
    val ragSources by vm.ragSources.collectAsState()
    val sessions by vm.sessions.collectAsState()

    var input by remember { mutableStateOf("") }
    var showSessions by remember { mutableStateOf(false) }
    var showParams by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, streamText) {
        if (messages.isNotEmpty() || streamText.isNotEmpty()) {
            listState.animateScrollToItem((messages.size + if (streamText.isNotEmpty()) 1 else 0).coerceAtLeast(0))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(session?.title.ifBlankTitle(), style = MaterialTheme.typography.titleMedium)
                        if (ctxUsed > 0) {
                            Text(
                                stringResource(R.string.chat_context_usage, ctxUsed, AppGraph.container.engines.chat.nCtx.takeIf { it > 0 } ?: 2048),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showSessions = true }) { Icon(Icons.Filled.History, null) }
                    IconButton(onClick = { vm.newSession() }) { Icon(Icons.Filled.Add, null) }
                    IconButton(onClick = { showParams = true }) { Icon(Icons.Filled.Tune, null) }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty() && streamText.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.chat_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(messages, key = { it.id }) { m ->
                    MessageBubble(
                        m = m,
                        onCopy = { clipboard.setText(AnnotatedString(m.content)) },
                        onSaveMemory = { vm.saveToMemory(m.content) },
                        onDelete = { vm.deleteMessage(m) },
                        onRegenerate = { vm.regenerate(m) }
                    )
                }
                if (streamText.isNotEmpty()) {
                    item {
                        Bubble(text = streamText, isUser = false, streaming = true)
                    }
                }
            }

            if (ragSources.isNotEmpty()) {
                Text(
                    stringResource(R.string.rag_sources, ragSources.joinToString(", ")),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            lastStats?.let { (tokens, tps) ->
                Text(
                    stringResource(R.string.chat_tokens_per_sec, tps, tokens),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            error?.let { e ->
                Text(
                    text = when {
                        e == "need_model" -> stringResource(R.string.chat_need_model_desc)
                        e.startsWith("load_failed") -> stringResource(R.string.chat_model_load_failed, e.removePrefix("load_failed: "))
                        else -> stringResource(R.string.chat_error, e)
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.chat_hint)) },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )
                if (generating) {
                    FilledIconButton(
                        onClick = { vm.stop() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Filled.Stop, null)
                    }
                } else {
                    FilledIconButton(
                        onClick = {
                            vm.send(input)
                            input = ""
                        },
                        enabled = input.isNotBlank(),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                    }
                }
            }
        }
    }

    if (showSessions) {
        AlertDialog(
            onDismissRequest = { showSessions = false },
            title = { Text(stringResource(R.string.chat_sessions)) },
            text = {
                Column {
                    if (sessions.isEmpty()) Text(stringResource(R.string.chat_empty))
                    LazyColumn {
                        items(sessions, key = { it.id }) { s ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            vm.openSession(s.id)
                                            showSessions = false
                                        },
                                        onLongClick = { vm.deleteSession(s.id) }
                                    )
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(s.title.ifBlank { "—" }, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSessions = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    if (showParams) {
        ParamsSheet(onDismiss = { showParams = false })
    }
}

private fun String?.ifBlankTitle(): String = this?.ifBlank { "" } ?: ""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParamsSheet(onDismiss: () -> Unit) {
    // Parameters dialog bound to persisted settings
    com.drs.ai.features.chat.GenParamsDialog(onDismiss = onDismiss)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    m: ChatMessage,
    onCopy: () -> Unit,
    onSaveMemory: () -> Unit,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .align(if (m.role == "user") Alignment.CenterEnd else Alignment.CenterStart)
                .widthIn(max = 320.dp)
                .combinedClickable(
                    onClick = { showMenu = !showMenu },
                    onLongClick = { showMenu = true }
                )
        ) {
            Bubble(text = m.content, isUser = m.role == "user", streaming = false)
        }
    }
    if (showMenu) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { onCopy(); showMenu = false }) { Text(stringResource(R.string.chat_copy)) }
            TextButton(onClick = { onSaveMemory(); showMenu = false }) { Text(stringResource(R.string.chat_save_memory)) }
            if (m.role == "assistant") {
                TextButton(onClick = { onRegenerate(); showMenu = false }) { Text(stringResource(R.string.chat_regenerate)) }
            }
            TextButton(onClick = { onDelete(); showMenu = false }) { Text(stringResource(R.string.delete)) }
            TextButton(onClick = { showMenu = false }) { Text(stringResource(R.string.close)) }
        }
    }
}

@Composable
private fun Bubble(text: String, isUser: Boolean, streaming: Boolean) {
    Card(
        shape = RoundedCornerShape(
            topStart = 20.dp, topEnd = 20.dp,
            bottomStart = if (isUser) 20.dp else 6.dp,
            bottomEnd = if (isUser) 6.dp else 20.dp
        ),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isUser) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        ),
        border = if (isUser) null else androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text + if (streaming) "▍" else "",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
