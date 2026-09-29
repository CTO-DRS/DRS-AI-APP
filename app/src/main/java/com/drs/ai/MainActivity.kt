package com.drs.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.drs.ai.data.settings.AppSettings
import com.drs.ai.ui.nav.DrsRoot
import com.drs.ai.ui.theme.DrsTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val settingsFlow by lazy {
        AppGraph.container.settings.settings.stateIn(
            lifecycleScope, SharingStarted.Eagerly, AppSettings()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val s by settingsFlow.collectAsState()
            DrsTheme(
                themeMode = s.themeMode,
                dynamicColors = s.dynamicColors,
                languageTag = s.language
            ) {
                DrsRoot()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            val s = AppGraph.container.settings.current()
            AppGraph.container.engines.unloadIfAuto(s.autoUnload)
        }
    }
}
