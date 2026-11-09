package com.imdadburki.aurelius

import com.imdadburki.aurelius.core.network.parseLrc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {
    @Test
    fun `parses timestamps and sorts`() {
        val lines = parseLrc("[00:12.00] Second line\n[00:01.50] First line\n[01:00.00] Third")
        assertEquals(3, lines.size)
        assertEquals(1_500L, lines[0].timeMs)
        assertEquals("First line", lines[0].text)
        assertEquals(12_000L, lines[1].timeMs)
        assertEquals(60_000L, lines[2].timeMs)
    }

    @Test
    fun `three digit fractions are milliseconds`() {
        val lines = parseLrc("[00:01.234] Precise")
        assertEquals(1_234L, lines[0].timeMs)
    }

    @Test
    fun `ignores metadata and empty lines`() {
        val lines = parseLrc("[ti:Title]\n[ar:Artist]\n\n[00:05.00]   \n[00:10.00] Real line")
        assertEquals(1, lines.size)
        assertEquals("Real line", lines[0].text)
    }

    @Test
    fun `empty input returns empty list`() {
        assertTrue(parseLrc("").isEmpty())
        assertTrue(parseLrc("no timestamps here").isEmpty())
    }
}
