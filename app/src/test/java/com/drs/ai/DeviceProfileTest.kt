package com.drs.ai

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM sanity checks for profile-related constants and quant labels.
 * (Hardware profiler itself requires Android context — covered by diagnostics on device.)
 */
class DeviceProfileTest {

    @Test
    fun quantLabelsMapCorrectly() {
        assertTrue(GgufParserQuant.check(15, "Q4_K_M"))
        assertTrue(GgufParserQuant.check(1, "F16"))
        assertTrue(GgufParserQuant.check(null, "unknown"))
    }

    private object GgufParserQuant {
        fun check(input: Long?, expected: String): Boolean =
            com.drs.ai.core.models.GgufParser.quantLabel(input) == expected
    }
}
