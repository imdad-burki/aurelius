package com.imdadburki.aurelius

import com.imdadburki.aurelius.core.data.displayAlbum
import com.imdadburki.aurelius.core.data.displayArtist
import com.imdadburki.aurelius.core.data.displayTitle
import com.imdadburki.aurelius.core.data.folderNameFromPath
import com.imdadburki.aurelius.core.data.folderNameFromRelativePath
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNameTest {
    @Test
    fun `unknown artist literal becomes Unknown Artist`() {
        assertEquals("Unknown Artist", displayArtist("<unknown>"))
        assertEquals("Unknown Artist", displayArtist("<UNKNOWN>"))
        assertEquals("Unknown Artist", displayArtist(null))
        assertEquals("Unknown Artist", displayArtist("   "))
    }

    @Test
    fun `real artist names pass through`() {
        assertEquals("Atif Aslam", displayArtist("Atif Aslam"))
        assertEquals("Bruno Mars", displayArtist("  Bruno Mars  "))
    }

    @Test
    fun `unknown album literal becomes Unknown Album`() {
        assertEquals("Unknown Album", displayAlbum("<unknown>"))
        assertEquals("Unknown Album", displayAlbum(null))
        assertEquals("Doorie", displayAlbum("Doorie"))
    }

    @Test
    fun `title falls back to file name without extension`() {
        assertEquals("song_one", displayTitle(null, "/storage/emulated/0/Music/song_one.mp3"))
        assertEquals("song_one", displayTitle("<unknown>", "/storage/emulated/0/Music/song_one.mp3"))
        assertEquals("Real Title", displayTitle("Real Title", "/storage/emulated/0/Music/song_one.mp3"))
        assertEquals("Unknown Title", displayTitle(null, null))
    }

    @Test
    fun `folder name is the parent directory name`() {
        assertEquals("Aurelius", folderNameFromPath("/storage/emulated/0/Music/Aurelius/song.mp3"))
        assertEquals("Download", folderNameFromPath("/storage/emulated/0/Download/x.mp3"))
        assertEquals("Music", folderNameFromPath("/storage/emulated/0/Music/song.mp3"))
    }

    @Test
    fun `folder name handles missing paths`() {
        assertEquals("Unknown folder", folderNameFromPath(null))
        assertEquals("Unknown folder", folderNameFromPath(""))
        assertEquals("Device storage", folderNameFromPath("/song.mp3"))
    }

    @Test
    fun `relative path folder is the last segment`() {
        assertEquals("Aurelius", folderNameFromRelativePath("Music/Aurelius/"))
        assertEquals("Music", folderNameFromRelativePath("Music/"))
        assertEquals("Unknown folder", folderNameFromRelativePath(null))
    }
}
