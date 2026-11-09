package com.imdadburki.aurelius

import com.imdadburki.aurelius.feature.home.greeting
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GreetingTest {
    @Test
    fun `greeting is one of the four plain greetings`() {
        val g = greeting()
        assertTrue(
            "unexpected greeting: $g",
            g in setOf("Good morning", "Good afternoon", "Good evening", "Good night")
        )
    }

    @Test
    fun `greeting never contains a name`() {
        assertFalse(greeting().contains("Imdad"))
    }
}
