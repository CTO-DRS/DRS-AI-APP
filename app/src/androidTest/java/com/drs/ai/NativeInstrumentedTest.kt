package com.drs.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeInstrumentedTest {

    @Test
    fun appContextAndDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.drs.ai", context.packageName)
        val db = com.drs.ai.data.db.DrsDatabase.get(context)
        assertTrue(db.openHelper.writableDatabase.isOpen)
    }

    @Test
    fun nativeLibrariesLoad() {
        assertTrue(com.drs.ai.core.inference.LlamaNative.available)
        assertTrue(com.drs.ai.core.vision.VisionNative.available)
        assertTrue(com.drs.ai.core.voice.WhisperNative.available)
    }

    @Test
    fun engineCreateDestroy() {
        val h = com.drs.ai.core.inference.LlamaNative.nativeCreate()
        assertTrue(h != 0L)
        com.drs.ai.core.inference.LlamaNative.nativeDestroy(h)
    }
}
