package com.imdadburki.aurelius

import com.imdadburki.aurelius.feature.player.bandFractionFromMillibels
import com.imdadburki.aurelius.feature.player.millibelsFromBandFraction
import org.junit.Assert.assertEquals
import org.junit.Test

class EqSliderMathTest {
    @Test
    fun `zero dB is the middle of the slider`() {
        assertEquals(0.5f, bandFractionFromMillibels(0), 0.001f)
    }

    @Test
    fun `ends map to the extremes`() {
        assertEquals(0f, bandFractionFromMillibels(-1500), 0.001f)
        assertEquals(1f, bandFractionFromMillibels(1500), 0.001f)
        assertEquals(-1500, millibelsFromBandFraction(0f))
        assertEquals(1500, millibelsFromBandFraction(1f))
        assertEquals(0, millibelsFromBandFraction(0.5f))
    }

    @Test
    fun `plus six dB sits above the middle`() {
        assertEquals(0.7f, bandFractionFromMillibels(600), 0.001f)
    }

    @Test
    fun `fraction round trips to the same millibels`() {
        listOf(-1500, -600, -100, 0, 100, 300, 1500).forEach { mb ->
            assertEquals("mb=$mb", mb, millibelsFromBandFraction(bandFractionFromMillibels(mb)))
        }
    }
}
