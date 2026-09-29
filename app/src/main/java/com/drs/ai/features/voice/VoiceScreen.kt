package com.drs.ai.features.voice

import android.Manifest
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.core.models.ModelRepository
import com.drs.ai.ui.components.GradientBanner
import com.drs.ai.ui.components.PulsingDot
import com.drs.ai.ui.components.PrimaryAction
import com.drs.ai.ui.components.SectionCard
import com.drs.ai.ui.components.SoftAction
import com.drs.ai.ui.components.entrance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VoiceScreen(nav: NavController) {
    val container = AppGraph.container
    val scope = rememberCoroutineScope()

    val allModels by container.models.observeModels().collectAsState(initial = emptyList())
    val sttModel = allModels.firstOrNull { it.kind == ModelRepository.KIND_WHISPER && it.active }

    var recording by remember { mutableStateOf(false) }
    var transcribing by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var duration by remember { mutableStateOf(0) }
    var langCode by remember { mutableStateOf("") }
    var pcmBuffer by remember { mutableStateOf<FloatArray?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        container.voice.initTts { ok -> ttsReady = ok }
        onDispose { container.voice.shutdownTts() }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            errorText = "mic_permission"
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GradientBanner(
            title = stringResource(R.string.voice_title),
            subtitle = sttModel?.name
        )

        if (sttModel == null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    stringResource(R.string.voice_need_model),
                    Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Column(Modifier.entrance(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (recording) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PulsingDot(MaterialTheme.colorScheme.error, size = 12.dp)
                    Text(
                        stringResource(R.string.voice_stop_recording),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            errorText?.let {
                Text(
                    when (it) {
                        "mic_permission" -> stringResource(R.string.voice_mic_permission)
                        else -> stringResource(R.string.voice_record_failed, it)
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!recording) {
                    PrimaryAction(
                        label = stringResource(R.string.voice_record),
                        icon = Icons.Filled.Mic,
                        onClick = {
                            errorText = null
                            if (container.voice.hasMicPermission()) {
                                recording = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val pcm = container.voice.record(60)
                                        pcmBuffer = pcm
                                        duration = (pcm.size / 16000)
                                    } catch (e: Exception) {
                                        errorText = e.message
                                    } finally {
                                        recording = false
                                    }
                                }
                            } else {
                                micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )
                } else {
                    PrimaryAction(
                        label = stringResource(R.string.voice_stop_recording),
                        icon = Icons.Filled.Stop,
                        onClick = { container.voice.recordingCancelled = true }
                    )
                }

                if (pcmBuffer != null && !recording) {
                    PrimaryAction(
                        label = if (transcribing) stringResource(R.string.voice_transcribing) else stringResource(R.string.voice_transcribe),
                        icon = Icons.Filled.GraphicEq,
                        enabled = !transcribing,
                        onClick = {
                            val model = sttModel ?: return@PrimaryAction
                            val pcm = pcmBuffer ?: return@PrimaryAction
                            transcribing = true
                            errorText = null
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        val err = container.voice.ensureLoaded(model.path, 4)
                                        if (err != null) throw IllegalStateException(err)
                                    }
                                    transcript = container.voice.transcribe(pcm, langCode.ifBlank { null }, false)
                                } catch (e: Exception) {
                                    errorText = e.message
                                } finally {
                                    transcribing = false
                                }
                            }
                        }
                    )
                }
            }

            if (duration > 0) {
                Text(
                    stringResource(R.string.voice_duration, duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = langCode,
                onValueChange = { langCode = it },
                label = { Text(stringResource(R.string.voice_lang)) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (transcript.isNotEmpty()) {
                SectionCard {
                    Text(
                        stringResource(R.string.voice_result),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(transcript, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(10.dp))
                    if (ttsReady) {
                        SoftAction(
                            label = stringResource(R.string.voice_speak),
                            icon = Icons.Filled.VolumeUp,
                            onClick = { container.voice.speak(transcript) }
                        )
                    } else {
                        Text(
                            stringResource(R.string.voice_tts_unavailable),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}
