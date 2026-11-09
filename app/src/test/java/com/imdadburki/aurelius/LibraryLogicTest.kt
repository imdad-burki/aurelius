package com.imdadburki.aurelius

import com.imdadburki.aurelius.core.data.Track
import com.imdadburki.aurelius.core.data.appendPositions
import com.imdadburki.aurelius.core.data.shouldAddFavorite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryLogicTest {
    @Test
    fun `favorite toggle adds when absent and removes when present`() {
        assertTrue(shouldAddFavorite(emptyList(), "local_1"))
        assertTrue(shouldAddFavorite(listOf("local_2"), "local_1"))
        assertFalse(shouldAddFavorite(listOf("local_1"), "local_1"))
    }

    @Test
    fun `favorite toggle round trip keeps state`() {
        val favorites = mutableSetOf<String>()
        val id = "local_7"
        // Add.
        if (shouldAddFavorite(favorites, id)) favorites.add(id) else favorites.remove(id)
        assertTrue(favorites.contains(id))
        // Tap again -> remove.
        if (shouldAddFavorite(favorites, id)) favorites.add(id) else favorites.remove(id)
        assertFalse(favorites.contains(id))
        // Tap a third time -> add again (state survives each navigation).
        if (shouldAddFavorite(favorites, id)) favorites.add(id) else favorites.remove(id)
        assertTrue(favorites.contains(id))
    }

    @Test
    fun `append positions continue after the last one`() {
        assertEquals(listOf(0, 1, 2), appendPositions(-1, 3))
        assertEquals(listOf(5, 6), appendPositions(4, 2))
        assertEquals(emptyList<Int>(), appendPositions(0, 0))
    }

    @Test
    fun `tracks group by real folder name`() {
        val tracks = listOf(
            Track("a", "One", "X", "Al", 1000, "u1", folderName = "Aurelius"),
            Track("b", "Two", "Y", "Al", 1000, "u2", folderName = "Download"),
            Track("c", "Three", "X", "Al", 1000, "u3", folderName = "Aurelius")
        )
        val grouped = tracks.groupBy { it.folderName.ifBlank { "Unknown folder" } }
        assertEquals(2, grouped.size)
        assertEquals(2, grouped["Aurelius"]?.size)
        assertEquals(1, grouped["Download"]?.size)
    }
}
