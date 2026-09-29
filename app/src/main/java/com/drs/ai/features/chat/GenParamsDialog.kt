package com.drs.ai.features.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.data.settings.GenParams
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Generation parameter tuning + presets, persisted in SettingsRepository. */
@Composable
fun GenParamsDialog(onDismiss: () -> Unit) {
    val container = AppGraph.container
    val s by container.settings.settings.collectAsState(initial = null)

    var temperature by remember { mutableFloatStateOf(0.7f) }
    var topK by remember { mutableIntStateOf(40) }
    var topP by remember { mutableFloatStateOf(0.95f) }
    var minP by remember { mutableFloatStateOf(0.05f) }
    var repeatPenalty by remember { mutableFloatStateOf(1.1f) }
    var maxTokens by remember { mutableIntStateOf(512) }
    var seedText by remember { mutableStateOf("-1") }
    var systemPrompt by remember { mutableStateOf("") }
    var replyLang by remember { mutableStateOf("") }
    var ragEnabled by remember { mutableStateOf(true) }
    var preset by remember { mutableStateOf("balanced") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val cur = container.settings.settings.first()
        temperature = cur.gen.temperature
        topK = cur.gen.topK
        topP = cur.gen.topP
        minP = cur.gen.minP
        repeatPenalty = cur.gen.repeatPenalty
        maxTokens = cur.gen.maxTokens
        seedText = cur.gen.seed.toString()
        systemPrompt = cur.systemPrompt
        replyLang = cur.replyLang
        ragEnabled = cur.ragEnabled
        preset = cur.preset
    }

    fun applyPreset(name: String): GenParams = when (name) {
        "precise" -> GenParams(0.15f, 20, 0.9f, 0.1f, 1.15f, maxTokens, seed = -1)
        "balanced" -> GenParams(0.7f, 40, 0.95f, 0.05f, 1.1f, maxTokens, seed = -1)
        "creative" -> GenParams(1.05f, 80, 0.97f, 0.02f, 1.05f, maxTokens, seed = -1)
        else -> GenParams(temperature, topK, topP, minP, repeatPenalty, maxTokens, seedText.toLongOrNull() ?: -1)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_params)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row {
                    for (p in listOf("precise", "balanced", "creative", "custom")) {
                        FilterChip(
                            selected = preset == p,
                            onClick = {
                                preset = p
                                val g = applyPreset(p)
                                temperature = g.temperature; topK = g.topK; topP = g.topP
                                minP = g.minP; repeatPenalty = g.repeatPenalty
                            },
                            label = {
                                Text(
                                    when (p) {
                                        "precise" -> stringResource(R.string.preset_precise)
                                        "balanced" -> stringResource(R.string.preset_balanced)
                                        "creative" -> stringResource(R.string.preset_creative)
                                        else -> stringResource(R.string.preset_custom)
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.chat_param_temperature) + " — %.2f".format(temperature))
                Slider(temperature, { temperature = it; preset = "custom" }, valueRange = 0f..1.5f)
                Text(stringResource(R.string.chat_param_top_k) + " — $topK")
                Slider(topK.toFloat(), { topK = it.toInt(); preset = "custom" }, valueRange = 0f..120f)
                Text(stringResource(R.string.chat_param_top_p) + " — %.2f".format(topP))
                Slider(topP, { topP = it; preset = "custom" }, valueRange = 0.05f..1f)
                Text(stringResource(R.string.chat_param_min_p) + " — %.2f".format(minP))
                Slider(minP, { minP = it; preset = "custom" }, valueRange = 0f..0.5f)
                Text(stringResource(R.string.chat_param_repeat) + " — %.2f".format(repeatPenalty))
                Slider(repeatPenalty, { repeatPenalty = it; preset = "custom" }, valueRange = 1f..1.5f)
                Text(stringResource(R.string.chat_param_max_tokens) + " — $maxTokens")
                Slider(maxTokens.toFloat(), { maxTokens = it.toInt(); preset = "custom" }, valueRange = 64f..2048f)
                OutlinedTextField(
                    value = seedText,
                    onValueChange = { seedText = it; preset = "custom" },
                    label = { Text(stringResource(R.string.chat_param_seed) + " (-1 = random)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text(stringResource(R.string.chat_system_prompt)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = replyLang,
                    onValueChange = { replyLang = it },
                    label = { Text(stringResource(R.string.chat_language_directive) + " (ar / en / …)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                FilterChip(
                    selected = ragEnabled,
                    onClick = { ragEnabled = !ragEnabled },
                    label = { Text(stringResource(R.string.chat_rag_enabled)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val g = GenParams(temperature, topK, topP, minP, repeatPenalty, maxTokens, seedText.toLongOrNull() ?: -1L)
                scope.launch {
                    container.settings.setGenParams(g)
                    container.settings.setPreset(preset)
                    container.settings.setSystemPrompt(systemPrompt)
                    container.settings.setReplyLang(replyLang)
                    container.settings.setRagEnabled(ragEnabled)
                }
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
