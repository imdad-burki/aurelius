package com.imdadburki.aurelius

import com.imdadburki.aurelius.core.design.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatDurationTest {
    @Test
    fun `zero and negative`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:00", formatDuration(-5000))
    }

    @Test
    fun `minutes and seconds`() {
        assertEquals("1:23", formatDuration(83_000))
        assertEquals("3:45", formatDuration(225_000))
    }

    @Test
    fun `hours`() {
        assertEquals("1:02:03", formatDuration(3_723_000))
    }
}
