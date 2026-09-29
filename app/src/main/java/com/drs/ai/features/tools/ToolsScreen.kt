package com.drs.ai.features.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.R
import com.drs.ai.core.tools.Tools
import com.drs.ai.ui.components.ChipRow
import com.drs.ai.ui.components.PrimaryAction
import com.drs.ai.ui.components.ScreenHeader
import com.drs.ai.ui.components.SectionCard
import com.drs.ai.ui.components.SoftAction
import com.drs.ai.ui.components.entrance

@Composable
fun ToolsScreen(nav: NavController) {
    val tabs = listOf(
        "calc" to R.string.tool_calculator,
        "units" to R.string.tool_units,
        "json" to R.string.tool_json,
        "regex" to R.string.tool_regex,
        "base64" to R.string.tool_base64,
        "hash" to R.string.tool_hash,
        "uuid" to R.string.tool_uuid
    )
    var selected by rememberSaveable { mutableStateOf("calc") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(title = stringResource(R.string.tools_title))

        Spacer(Modifier.height(10.dp))
        ChipRow(
            items = tabs.map { (id, res) -> id to stringResource(res) },
            selectedId = selected,
            onSelect = { selected = it }
        )
        Spacer(Modifier.height(12.dp))
        Column(Modifier.verticalScroll(rememberScrollState())) {
            when (selected) {
                "calc" -> CalculatorTool()
                "units" -> UnitsTool()
                "json" -> JsonToolUi()
                "regex" -> RegexToolUi()
                "base64" -> Base64ToolUi()
                "hash" -> HashToolUi()
                "uuid" -> UuidToolUi()
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ResultCard(text: String) {
    SectionCard {
        Text(
            text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CalculatorTool() {
    var expr by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    val calcErr = stringResource(R.string.calc_error)
    Column(Modifier.entrance(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = expr, onValueChange = { expr = it },
            label = { Text("2+2*3, sqrt(2), sin(30), pi^2") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        PrimaryAction(
            label = stringResource(R.string.calc_result),
            onClick = { result = try { Tools.calculate(expr) } catch (e: Exception) { calcErr } }
        )
        if (result.isNotEmpty()) ResultCard(result)
    }
}

@Composable
private fun UnitsTool() {
    var category by remember { mutableStateOf("temperature") }
    var from by remember { mutableStateOf("C") }
    var to by remember { mutableStateOf("F") }
    var value by remember { mutableStateOf("25") }
    var output by remember { mutableStateOf("") }
    val calcErr = stringResource(R.string.calc_error)

    Column(Modifier.entrance(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChipRow(
            items = listOf(
                "length" to R.string.units_length, "mass" to R.string.units_mass,
                "temperature" to R.string.units_temperature, "area" to R.string.units_area,
                "volume" to R.string.units_volume, "speed" to R.string.units_speed,
                "data" to R.string.units_data, "time" to R.string.units_time,
                "pressure" to R.string.units_pressure, "energy" to R.string.units_energy
            ).map { (id, res) -> id to stringResource(res) },
            selectedId = category,
            onSelect = { id ->
                category = id
                val units = Tools.categories[id]!!
                from = units.first().id
                to = units[1].id
                output = ""
            }
        )
        val units = Tools.categories[category]!!
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = from, onValueChange = { from = it }, label = { Text(stringResource(R.string.units_from)) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
            OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("= ?") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
            OutlinedTextField(value = to, onValueChange = { to = it }, label = { Text(stringResource(R.string.units_to)) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
        }
        Text(
            units.joinToString(", ") { "${it.id} (${it.label})" },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PrimaryAction(
            label = stringResource(R.string.calc_result),
            onClick = {
                output = try {
                    val v = value.replace(',', '.').toDouble()
                    Tools.convert(category, from.trim(), to.trim(), v).toString()
                } catch (e: Exception) {
                    calcErr
                }
            }
        )
        if (output.isNotEmpty()) ResultCard(output)
    }
}

@Composable
private fun JsonToolUi() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    val invalid = stringResource(R.string.json_invalid, "")
    Column(Modifier.entrance(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("JSON") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(140.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftAction(
                label = stringResource(R.string.json_pretty),
                onClick = { output = try { Tools.JsonTool.pretty(input) } catch (e: Exception) { invalid + (e.message ?: "") } }
            )
            SoftAction(
                label = stringResource(R.string.json_minify),
                onClick = { output = try { Tools.JsonTool.minify(input) } catch (e: Exception) { invalid + (e.message ?: "") } }
            )
        }
        if (output.isNotEmpty()) {
            SectionCard {
                Text(output, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun RegexToolUi() {
    var pattern by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    val invalid = stringResource(R.string.regex_invalid, "")
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.entrance(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text(stringResource(R.string.regex_pattern)) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.regex_test_text)) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(120.dp))
        PrimaryAction(
            label = stringResource(R.string.tool_regex),
            onClick = {
                output = try {
                    val ms = Tools.RegexTool.test(pattern, text)
                    ctx.getString(R.string.regex_matches, ms.size) + "\n" + ms.take(20).joinToString("\n") { "[${it.start}..${it.end}] ${it.text}" }
                } catch (e: Exception) {
                    invalid + (e.message ?: "")
                }
            }
        )
        if (output.isNotEmpty()) SectionCard { Text(output, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun Base64ToolUi() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    val clip = LocalClipboardManager.current
    val invalid = stringResource(R.string.base64_invalid)
    Column(Modifier.entrance(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Base64") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(120.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftAction(label = stringResource(R.string.base64_encode), onClick = { output = Tools.Base64Tool.encode(input) })
            SoftAction(
                label = stringResource(R.string.base64_decode),
                onClick = { output = try { Tools.Base64Tool.decode(input) } catch (e: Exception) { invalid } }
            )
            SoftAction(label = stringResource(R.string.tool_copied), onClick = { clip.setText(AnnotatedString(output)) })
        }
        if (output.isNotEmpty()) SectionCard { Text(output, maxLines = 8) }
    }
}

@Composable
private fun HashToolUi() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf(emptyMap<String, String>()) }
    Column(Modifier.entrance(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text(stringResource(R.string.hash_input)) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        PrimaryAction(label = stringResource(R.string.hash_compute), onClick = { output = Tools.HashTool.all(input) })
        for ((k, v) in output) {
            Text("$k:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(v, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
    }
}

@Composable
private fun UuidToolUi() {
    var count by remember { mutableStateOf("5") }
    var output by remember { mutableStateOf(emptyList<String>()) }
    val clip = LocalClipboardManager.current
    Column(Modifier.entrance(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = count, onValueChange = { count = it }, label = { Text(stringResource(R.string.uuid_count)) }, shape = RoundedCornerShape(14.dp))
        PrimaryAction(label = stringResource(R.string.uuid_generate), onClick = { output = Tools.UuidTool.generate(count.toIntOrNull() ?: 5) })
        if (output.isNotEmpty()) {
            SoftAction(label = stringResource(R.string.tool_copied), onClick = { clip.setText(AnnotatedString(output.joinToString("\n"))) })
            for (u in output) Text(u, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
