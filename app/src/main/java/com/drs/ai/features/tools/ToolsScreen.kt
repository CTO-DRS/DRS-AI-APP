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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.drs.ai.R
import com.drs.ai.core.tools.Tools

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
        Text(stringResource(R.string.tools_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScrollable()) {
            for ((id, label) in tabs) {
                FilterChip(
                    selected = selected == id,
                    onClick = { selected = id },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
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
private fun Modifier.horizontalScrollable(): Modifier = this.then(Modifier)

@Composable
private fun CalculatorTool() {
    var expr by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    val calcErr = stringResource(R.string.calc_error)
    OutlinedTextField(
        value = expr, onValueChange = { expr = it },
        label = { Text("2+2*3, sqrt(2), sin(30), pi^2") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = {
        result = try { Tools.calculate(expr) } catch (e: Exception) { calcErr }
    }) { Text(stringResource(R.string.calc_result)) }
    if (result.isNotEmpty()) {
        Text(result, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
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

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            for ((id, labelRes) in listOf(
                "length" to R.string.units_length, "mass" to R.string.units_mass,
                "temperature" to R.string.units_temperature, "area" to R.string.units_area,
                "volume" to R.string.units_volume, "speed" to R.string.units_speed,
                "data" to R.string.units_data, "time" to R.string.units_time,
                "pressure" to R.string.units_pressure, "energy" to R.string.units_energy
            )) {
                FilterChip(
                    selected = category == id,
                    onClick = {
                        category = id
                        val units = Tools.categories[id]!!
                        from = units.first().id
                        to = units[1].id
                        output = ""
                    },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val units = Tools.categories[category]!!
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = from, onValueChange = { from = it }, label = { Text(stringResource(R.string.units_from)) }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("= ?") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = to, onValueChange = { to = it }, label = { Text(stringResource(R.string.units_to)) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            units.joinToString(", ") { "${it.id} (${it.label})" },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            output = try {
                val v = value.replace(',', '.').toDouble()
                Tools.convert(category, from.trim(), to.trim(), v).toString()
            } catch (e: Exception) {
                calcErr
            }
        }) { Text(stringResource(R.string.calc_result)) }
        if (output.isNotEmpty()) Text(output, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun JsonToolUi() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    val invalid = stringResource(R.string.json_invalid, "")
    OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("JSON") }, modifier = Modifier.fillMaxWidth().height(140.dp))
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { output = try { Tools.JsonTool.pretty(input) } catch (e: Exception) { invalid + (e.message ?: "") } }) {
            Text(stringResource(R.string.json_pretty))
        }
        OutlinedButton(onClick = { output = try { Tools.JsonTool.minify(input) } catch (e: Exception) { invalid + (e.message ?: "") } }) {
            Text(stringResource(R.string.json_minify))
        }
    }
    if (output.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Card { Text(output, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun RegexToolUi() {
    var pattern by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    val invalid = stringResource(R.string.regex_invalid, "")
    val ctx = androidx.compose.ui.platform.LocalContext.current
    OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text(stringResource(R.string.regex_pattern)) }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.regex_test_text)) }, modifier = Modifier.fillMaxWidth().height(120.dp))
    Spacer(Modifier.height(8.dp))
    Button(onClick = {
        output = try {
            val ms = Tools.RegexTool.test(pattern, text)
            ctx.getString(R.string.regex_matches, ms.size) + "\n" + ms.take(20).joinToString("\n") { "[${it.start}..${it.end}] ${it.text}" }
        } catch (e: Exception) {
            invalid + (e.message ?: "")
        }
    }) { Text(stringResource(R.string.tool_regex)) }
    if (output.isNotEmpty()) Text(output, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Base64ToolUi() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    val clip = LocalClipboardManager.current
    val invalid = stringResource(R.string.base64_invalid)
    OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Base64") }, modifier = Modifier.fillMaxWidth().height(120.dp))
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { output = Tools.Base64Tool.encode(input) }) { Text(stringResource(R.string.base64_encode)) }
        OutlinedButton(onClick = {
            output = try { Tools.Base64Tool.decode(input) } catch (e: Exception) { invalid }
        }) { Text(stringResource(R.string.base64_decode)) }
        OutlinedButton(onClick = { clip.setText(AnnotatedString(output)) }) { Text(stringResource(R.string.tool_copied)) }
    }
    if (output.isNotEmpty()) Card { Text(output, Modifier.padding(12.dp), maxLines = 8) }
}

@Composable
private fun HashToolUi() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf(emptyMap<String, String>()) }
    OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text(stringResource(R.string.hash_input)) }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Button(onClick = { output = Tools.HashTool.all(input) }) { Text(stringResource(R.string.hash_compute)) }
    for ((k, v) in output) {
        Text("$k:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(v, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
    }
}

@Composable
private fun UuidToolUi() {
    var count by remember { mutableStateOf("5") }
    var output by remember { mutableStateOf(emptyList<String>()) }
    val clip = LocalClipboardManager.current
    OutlinedTextField(value = count, onValueChange = { count = it }, label = { Text(stringResource(R.string.uuid_count)) })
    Spacer(Modifier.height(8.dp))
    Button(onClick = { output = Tools.UuidTool.generate(count.toIntOrNull() ?: 5) }) { Text(stringResource(R.string.uuid_generate)) }
    if (output.isNotEmpty()) {
        OutlinedButton(onClick = { clip.setText(AnnotatedString(output.joinToString("\n"))) }) { Text(stringResource(R.string.tool_copied)) }
        for (u in output) Text(u, style = MaterialTheme.typography.bodyMedium)
    }
}
