package com.imdadburki.aurelius

import com.imdadburki.aurelius.core.network.parseIso8601Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class Iso8601DurationTest {
    @Test
    fun `minutes and seconds`() {
        assertEquals(268_000L, parseIso8601Duration("PT4M28S"))
    }

    @Test
    fun `hours minutes seconds`() {
        assertEquals(3_723_000L, parseIso8601Duration("PT1H2M3S"))
    }

    @Test
    fun `seconds only`() {
        assertEquals(45_000L, parseIso8601Duration("PT45S"))
    }

    @Test
    fun `zero duration`() {
        assertEquals(0L, parseIso8601Duration("PT0S"))
    }

    @Test
    fun `invalid input returns zero`() {
        assertEquals(0L, parseIso8601Duration("not-a-duration"))
        assertEquals(0L, parseIso8601Duration(""))
    }
}
