package com.drs.ai

import android.app.Application
import com.drs.ai.core.diagnostics.DiagnosticsCollector
import com.drs.ai.core.inference.EngineManager
import com.drs.ai.core.memory.MemoryManager
import com.drs.ai.core.models.ModelRepository
import com.drs.ai.core.rag.RagPipeline
import com.drs.ai.core.vision.VisionEngine
import com.drs.ai.core.voice.VoiceEngine
import com.drs.ai.data.db.DrsDatabase
import com.drs.ai.data.settings.SettingsRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/** Manual DI container — no network, no analytics, nothing hidden. */
class AppContainer(app: Application) {
    val appContext: android.content.Context = app.applicationContext
    val settings = SettingsRepository(app)
    val db = DrsDatabase.get(app)
    val engines = EngineManager()
    val models = ModelRepository(app, db.memoryAndModelsDao(), settings)
    val memory = MemoryManager(db.memoryAndModelsDao())
    val rag = RagPipeline(app, db.ragDao(), engines.embedder)
    val vision = VisionEngine(app, engines.chat)
    val voice = VoiceEngine(app)
    val diagnostics = DiagnosticsCollector(app, engines.chat, engines.embedder)
}

object AppGraph {
    @Volatile lateinit var container: AppContainer
        private set

    fun init(app: DrsApp): AppContainer {
        if (!::container.isInitialized) container = AppContainer(app)
        return container
    }
}

class DrsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        AppGraph.init(this)
    }
}
