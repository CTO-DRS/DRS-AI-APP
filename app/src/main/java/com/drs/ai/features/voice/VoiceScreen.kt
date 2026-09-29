package com.drs.ai.features.voice

import android.Manifest
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.core.models.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VoiceScreen(nav: NavController) {
    val container = AppGraph.container
    val context = LocalContext.current
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.voice_title), style = MaterialTheme.typography.titleLarge)

        if (sttModel == null) {
            Card {
                Text(stringResource(R.string.voice_need_model), Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
            }
        }

        if (recording) {
            Text(stringResource(R.string.voice_stop_recording), color = MaterialTheme.colorScheme.primary)
        }
        errorText?.let {
            Text(
                when (it) {
                    "mic_permission" -> stringResource(R.string.voice_mic_permission)
                    else -> stringResource(R.string.voice_record_failed, it)
                },
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!recording) {
                Button(onClick = {
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
                }) { Text(stringResource(R.string.voice_record)) }
            } else {
                Button(onClick = { container.voice.recordingCancelled = true }) {
                    Text(stringResource(R.string.voice_stop_recording))
                }
            }

            if (pcmBuffer != null && !recording) {
                OutlinedButton(onClick = {
                    val model = sttModel ?: return@OutlinedButton
                    val pcm = pcmBuffer ?: return@OutlinedButton
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
                }, enabled = !transcribing) {
                    Text(if (transcribing) stringResource(R.string.voice_transcribing) else stringResource(R.string.voice_transcribe))
                }
            }
        }

        if (duration > 0) {
            Text(stringResource(R.string.voice_duration, duration), style = MaterialTheme.typography.labelMedium)
        }

        OutlinedTextField(
            value = langCode,
            onValueChange = { langCode = it },
            label = { Text(stringResource(R.string.voice_lang)) },
            modifier = Modifier.fillMaxWidth()
        )

        if (transcript.isNotEmpty()) {
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.voice_result), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(transcript, style = MaterialTheme.typography.bodyLarge)
                    if (ttsReady) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { container.voice.speak(transcript) }) {
                            Text(stringResource(R.string.voice_speak))
                        }
                    } else {
                        Text(stringResource(R.string.voice_tts_unavailable), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
