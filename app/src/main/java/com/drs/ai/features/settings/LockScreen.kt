package com.drs.ai.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drs.ai.R

/** PIN gate shown over the app while locked. */
@Composable
fun LockScreen(storedHash: String, storedSalt: String, onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.lock_enter_pin), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter { c -> c.isDigit() }.take(8); wrong = false },
            label = { Text(stringResource(R.string.lock_enter_pin)) },
            modifier = Modifier.fillMaxWidth()
        )
        if (wrong) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.lock_wrong), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (PinCrypto.verify(pin, storedSalt, storedHash)) onUnlocked() else wrong = true
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.lock_unlock)) }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.lock_use_biometrics_note), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
