package com.drs.ai.features.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.core.models.ModelRepository
import kotlinx.coroutines.launch

@Composable
fun VisionScreen(nav: NavController) {
    val container = AppGraph.container
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var question by remember { mutableStateOf("") }
    val answerState = remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var answer by answerState

    val allModels by container.models.observeModels().collectAsState(initial = emptyList())
    val chatModel = allModels.firstOrNull { it.kind == ModelRepository.KIND_CHAT && it.active }
    val mmproj = allModels.firstOrNull { it.kind == ModelRepository.KIND_MMPROJ && it.active }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
        answerState.value = ""
        errorText = null
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.vision_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.vision_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Honest 2-file requirement display
        if (chatModel == null) {
            Card {
                Text(
                    stringResource(R.string.vision_need_two_models),
                    Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (mmproj == null) {
            Card {
                Text(
                    stringResource(R.string.vision_need_mmproj),
                    Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        OutlinedButton(onClick = { pickImage.launch("image/*") }) {
            Text(stringResource(R.string.vision_pick))
        }
        imageUri?.let { uri ->
            val painter = remember(uri) {
                val bmp = container.appContext.contentResolver.openInputStream(uri)?.use { ins ->
                    BitmapFactory.decodeStream(ins)
                } ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                BitmapPainter(bmp.asImageBitmap())
            }
            Image(painter, contentDescription = null, modifier = Modifier.size(220.dp))
        }

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text(stringResource(R.string.vision_ask)) },
            modifier = Modifier.fillMaxWidth()
        )

        if (running) Text(stringResource(R.string.vision_running), color = MaterialTheme.colorScheme.primary)
        errorText?.let { Text(stringResource(R.string.vision_failed, it), color = MaterialTheme.colorScheme.error) }

        if (answer.isNotEmpty()) {
            Card {
                Text(answer, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }

        val canRun = imageUri != null && chatModel != null && mmproj != null && !running
        Button(
            onClick = {
                val chat = chatModel ?: return@Button
                val proj = mmproj ?: return@Button
                val uri = imageUri ?: return@Button
                running = true
                errorText = null
                answerState.value = ""
                scope.launch {
                    try {
                        val err = container.vision.ensureLoaded(chat.path, proj.path, 2048, 4)
                        if (err != null) {
                            errorText = err
                        } else {
                            container.vision.analyze(uri, question.ifBlank { "Describe this image." }) { piece ->
                                answerState.value += piece
                            }
                        }
                    } catch (e: Exception) {
                        errorText = e.message
                    } finally {
                        running = false
                    }
                }
            },
            enabled = canRun
        ) {
            Text(stringResource(R.string.vision_ask))
        }
        Spacer(Modifier.height(24.dp))
    }
}
