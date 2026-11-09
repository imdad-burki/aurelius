package com.imdadburki.aurelius

import com.imdadburki.aurelius.feature.player.EqPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqPresetsTest {
    @Test
    fun `every preset has ten bands`() {
        EqPresets.names.forEach { name ->
            assertEquals("preset $name", 10, EqPresets.levels(name).size)
        }
    }

    @Test
    fun `flat preset is all zeros`() {
        assertTrue(EqPresets.levels("Flat").all { it == 0 })
    }

    @Test
    fun `bass boost raises low bands`() {
        val bass = EqPresets.levels("Bass Boost")
        assertTrue(bass[0] > 0)
        assertTrue(bass[0] >= bass[9])
    }

    @Test
    fun `unknown preset falls back to flat`() {
        assertEquals(EqPresets.levels("Flat"), EqPresets.levels("No Such Preset"))
    }

    @Test
    fun `levels stay within equalizer range`() {
        EqPresets.names.forEach { name ->
            EqPresets.levels(name).forEach { level ->
                assertTrue("$name level $level out of range", level in -1500..1500)
            }
        }
    }
}
