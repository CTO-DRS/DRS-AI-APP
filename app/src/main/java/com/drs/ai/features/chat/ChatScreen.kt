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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import android.widget.Toast
import com.drs.ai.domain.ChatExporter
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.data.db.ChatMessage
import com.drs.ai.data.db.ChatSession
import com.drs.ai.data.db.PromptTemplate
import com.drs.ai.ui.components.TypingIndicator
import com.drs.ai.ui.components.entrance
import com.drs.ai.ui.components.pressScale
import com.drs.ai.ui.components.streamingText
import com.drs.ai.ui.components.PulsingDot
import androidx.compose.foundation.interaction.MutableInteractionSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(nav: NavController, vm: ChatViewModel = viewModel(factory = ChatViewModel.factory())) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current

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
    val templates by vm.templates.collectAsState()

    var input by remember { mutableStateOf("") }
    var showSessions by remember { mutableStateOf(false) }
    var showParams by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var exportToast by remember { mutableStateOf(false) }

    fun doExport(fmt: ChatExporter.Format) {
        val msgs = messages
        val sess = session ?: return
        if (msgs.isEmpty()) return
        try {
            val file = ChatExporter.export(context, sess, msgs, fmt)
            ChatExporter.share(context, file)
        } catch (t: Throwable) {
            Toast.makeText(context, t.message ?: "export failed", Toast.LENGTH_SHORT).show()
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(exportToast) {
        if (exportToast) {
            Toast.makeText(context, context.getString(R.string.export_share_toast), Toast.LENGTH_SHORT).show()
            exportToast = false
        }
    }
    LaunchedEffect(notice) {
        val n = notice ?: return@LaunchedEffect
        val res = when (n) {
            "context_trimmed" -> R.string.notice_context_trimmed
            "context_compressed" -> R.string.notice_context_compressed
            "loading_model" -> R.string.notice_loading_model
            "memory_saved" -> R.string.notice_memory_saved
            else -> return@LaunchedEffect
        }
        Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show()
    }
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
                        Text(session?.title?.ifBlank { stringResource(R.string.app_name) } ?: "", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (ctxUsed > 0) {
                                Text(
                                    stringResource(R.string.chat_context_usage, ctxUsed, AppGraph.container.engines.chat.nCtx.takeIf { it > 0 } ?: 2048),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // v1.4: honest compression indicator — user always knows
                            if (session?.summary != null) {
                                Icon(
                                    Icons.Filled.AutoAwesome, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    stringResource(R.string.chat_compressed_badge),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showTemplates = true }) { Icon(Icons.Filled.AutoAwesome, null) }
                    IconButton(onClick = { showSessions = true }) { Icon(Icons.Filled.History, null) }
                    IconButton(onClick = { vm.newSession() }) { Icon(Icons.Filled.Add, null) }
                    IconButton(onClick = { showParams = true }) { Icon(Icons.Filled.Tune, null) }
                    // v1.3: improved export
                    IconButton(onClick = { showExport = true }) {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                    }
                    DropdownMenu(expanded = showExport, onDismissRequest = { showExport = false }) {
                        Text(
                            stringResource(R.string.export_menu_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_fmt_markdown)) },
                            onClick = {
                                showExport = false
                                doExport(ChatExporter.Format.Markdown)
                                exportToast = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_fmt_html)) },
                            onClick = {
                                showExport = false
                                doExport(ChatExporter.Format.Html)
                                exportToast = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_fmt_pdf)) },
                            onClick = {
                                showExport = false
                                doExport(ChatExporter.Format.Pdf)
                                exportToast = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_fmt_json)) },
                            onClick = {
                                showExport = false
                                doExport(ChatExporter.Format.Json)
                                exportToast = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_fmt_txt)) },
                            onClick = {
                                showExport = false
                                doExport(ChatExporter.Format.Txt)
                                exportToast = true
                            }
                        )
                    }
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
                        Bubble(annotated = streamingText(streamText), isUser = false, streaming = true)
                    }
                } else if (generating) {
                    // v1.4: thinking dots while the model warms up before the first token
                    item {
                        Box(
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    RoundedCornerShape(
                                        topStart = 20.dp, topEnd = 20.dp,
                                        bottomStart = 6.dp, bottomEnd = 20.dp
                                    )
                                )
                        ) { TypingIndicator() }
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
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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

    // ── Sessions manager (v1.4): search + pin + rename + delete ──────────────
    if (showSessions) {
        SessionsDialog(
            sessions = sessions,
            onDismiss = { showSessions = false },
            onOpen = {
                vm.openSession(it.id)
                showSessions = false
            },
            onPin = { s -> vm.setPinned(s.id, !s.pinned) },
            onRename = { s, t -> vm.renameSession(s.id, t) },
            onDelete = { s -> vm.deleteSession(s.id) }
        )
    }

    // ── Prompt template library (v1.4) ──────────────────────────────────────
    if (showTemplates) {
        TemplatesSheet(
            templates = templates,
            onDismiss = { showTemplates = false },
            onPick = { t ->
                vm.onTemplateUsed(t.id)
                input = if (input.isBlank()) t.content else input + " " + t.content
                showTemplates = false
            },
            onSave = { title, content, cat -> vm.addTemplate(title, content, cat) },
            onUpdate = { t, title, content, cat -> vm.updateTemplate(t, title, content, cat) },
            onDelete = { vm.deleteTemplate(it) }
        )
    }

    if (showParams) {
        ParamsSheet(onDismiss = { showParams = false })
    }
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
            Column {
                Bubble(text = m.content, isUser = m.role == "user", streaming = false)
                // v1.4: per-message speed badge — honest performance, visible inline
                if (m.role == "assistant" && m.tokPerSec > 0f) {
                    Text(
                        String.format("⚡ %.1f tok/s", m.tokPerSec),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
            }
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
private fun Bubble(
    text: String? = null,
    annotated: AnnotatedString? = null,
    isUser: Boolean,
    streaming: Boolean
) {
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
        border = if (isUser) null else BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            annotated ?: AnnotatedString(text ?: ""),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// v1.4 — Sessions manager dialog: search / pin / rename / delete
// ═══════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionsDialog(
    sessions: List<ChatSession>,
    onDismiss: () -> Unit,
    onOpen: (ChatSession) -> Unit,
    onPin: (ChatSession) -> Unit,
    onRename: (ChatSession, String) -> Unit,
    onDelete: (ChatSession) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<ChatSession?>(null) }
    var renameText by remember { mutableStateOf("") }

    val filtered = if (query.isBlank()) sessions
    else sessions.filter { it.title.contains(query, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_sessions)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.sessions_search_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text(
                        stringResource(R.string.sessions_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(filtered, key = { it.id }) { s ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onOpen(s) },
                                    onLongClick = {
                                        renaming = s
                                        renameText = s.title
                                    }
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (s.pinned) {
                                Icon(
                                    Icons.Filled.PushPin, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                s.title.ifBlank { "—" },
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(
                                onClick = { onPin(s) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    if (s.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = stringResource(R.string.sessions_pin),
                                    tint = if (s.pinned) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { renaming = s; renameText = s.title },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Edit, contentDescription = stringResource(R.string.sessions_rename),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDelete(s) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete, contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.sessions_longpress_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )

    renaming?.let { target ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.sessions_rename)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) onRename(target, renameText)
                        renaming = null
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// v1.4 — Prompt template library bottom sheet
// ═══════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatesSheet(
    templates: List<PromptTemplate>,
    onDismiss: () -> Unit,
    onPick: (PromptTemplate) -> Unit,
    onSave: (String, String, String) -> Unit,
    onUpdate: (PromptTemplate, String, String, String) -> Unit,
    onDelete: (PromptTemplate) -> Unit
) {
    var editing by remember { mutableStateOf<PromptTemplate?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoAwesome, null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.templates_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showEditor = true }) {
                    Text(stringResource(R.string.templates_new))
                }
            }
            Text(
                stringResource(R.string.templates_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (templates.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.templates_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }
            items(templates, key = { it.id }) { t ->
                val interaction = remember { MutableInteractionSource() }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(interactionSource = interaction),
                    onClick = { onPick(t) },
                    interactionSource = interaction,
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                t.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (t.category.isNotBlank()) {
                                Text(
                                    t.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { editing = t; showEditor = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Edit, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDelete(t) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete, null,
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Text(
                            t.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        var title by remember(showEditor) { mutableStateOf(editing?.title ?: "") }
        var content by remember(showEditor) { mutableStateOf(editing?.content ?: "") }
        var category by remember(showEditor) { mutableStateOf(editing?.category ?: "") }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = {
                Text(
                    if (editing == null) stringResource(R.string.templates_new)
                    else stringResource(R.string.templates_edit)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text(stringResource(R.string.templates_field_title)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content, onValueChange = { content = it },
                        label = { Text(stringResource(R.string.templates_field_content)) },
                        minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category, onValueChange = { category = it },
                        label = { Text(stringResource(R.string.templates_field_category)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank() && content.isNotBlank(),
                    onClick = {
                        val e = editing
                        if (e == null) onSave(title, content, category)
                        else onUpdate(e, title, content, category)
                        showEditor = false
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParamsSheet(onDismiss: () -> Unit) {
    // Parameters dialog bound to persisted settings
    com.drs.ai.features.chat.GenParamsDialog(onDismiss = onDismiss)
}
