package com.drs.ai.features.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.AppGraph
import com.drs.ai.R
import com.drs.ai.core.models.ModelRepository
import com.drs.ai.ui.components.GradientBanner
import com.drs.ai.ui.components.IconBadge
import com.drs.ai.ui.components.PrimaryAction
import com.drs.ai.ui.components.SectionCard
import com.drs.ai.ui.components.SoftAction
import com.drs.ai.ui.components.StatusBadge
import com.drs.ai.ui.components.entrance
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GradientBanner(
            title = stringResource(R.string.vision_title),
            subtitle = stringResource(R.string.vision_desc)
        )

        // ── Requirement status (honest two-file requirement) ─────────────
        Column(Modifier.entrance(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (chatModel == null) {
                RequirementRow(
                    text = stringResource(R.string.vision_need_two_models),
                    severity = RequirementSeverity.ERROR
                )
            } else if (mmproj == null) {
                RequirementRow(
                    text = stringResource(R.string.vision_need_mmproj),
                    severity = RequirementSeverity.WARN
                )
            } else {
                RequirementRow(
                    text = chatModel.name + " + " + mmproj.name,
                    severity = RequirementSeverity.OK
                )
            }
        }

        // ── Image picker ─────────────────────────────────────────────────
        SoftAction(
            label = stringResource(R.string.vision_pick),
            icon = Icons.Filled.Image,
            onClick = { pickImage.launch("image/*") }
        )
        imageUri?.let { uri ->
            val painter = remember(uri) {
                val bmp = container.appContext.contentResolver.openInputStream(uri)?.use { ins ->
                    BitmapFactory.decodeStream(ins)
                } ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                BitmapPainter(bmp.asImageBitmap())
            }
            Image(
                painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
        }

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text(stringResource(R.string.vision_ask)) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (running) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.drs.ai.ui.components.PulsingDot(MaterialTheme.colorScheme.secondary)
                Text(
                    stringResource(R.string.vision_running),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        errorText?.let {
            Text(
                stringResource(R.string.vision_failed, it),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (answer.isNotEmpty()) {
            SectionCard(title = null) {
                Text(answer, style = MaterialTheme.typography.bodyLarge)
            }
        }

        PrimaryAction(
            label = stringResource(R.string.vision_ask),
            icon = Icons.Filled.AutoAwesome,
            enabled = imageUri != null && chatModel != null && mmproj != null && !running,
            onClick = {
                val chat = chatModel ?: return@PrimaryAction
                val proj = mmproj ?: return@PrimaryAction
                val uri = imageUri ?: return@PrimaryAction
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
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(48.dp))
    }
}

private enum class RequirementSeverity { OK, WARN, ERROR }

@Composable
private fun RequirementRow(text: String, severity: RequirementSeverity) {
    val (icon, tint) = when (severity) {
        RequirementSeverity.OK -> Icons.Filled.AutoAwesome to MaterialTheme.colorScheme.primary
        RequirementSeverity.WARN -> Icons.Filled.Warning to MaterialTheme.colorScheme.tertiary
        RequirementSeverity.ERROR -> Icons.Filled.Warning to MaterialTheme.colorScheme.error
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = tint.copy(alpha = 0.10f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconBadge(
                icon = icon,
                container = tint.copy(alpha = 0.18f),
                content = tint,
                size = 34.dp,
                corner = 10.dp
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = tint,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
