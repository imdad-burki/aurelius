package com.imdadburki.aurelius.core.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

/**
 * Display-name rules (pure, unit-tested): MediaStore returns the literal string
 * "<unknown>" when a file has no artist/album tag — never show that to the user.
 */
fun displayArtist(raw: String?): String {
    val t = raw?.trim().orEmpty()
    return if (t.isEmpty() || t.equals("<unknown>", ignoreCase = true)) "Unknown Artist" else t
}

fun displayAlbum(raw: String?): String {
    val t = raw?.trim().orEmpty()
    return if (t.isEmpty() || t.equals("<unknown>", ignoreCase = true)) "Unknown Album" else t
}

fun displayTitle(raw: String?, fallbackFileName: String? = null): String {
    val t = raw?.trim().orEmpty()
    if (t.isNotEmpty() && !t.equals("<unknown>", ignoreCase = true)) return t
    val f = fallbackFileName?.substringAfterLast('/')?.substringBeforeLast('.')?.trim().orEmpty()
    return f.ifEmpty { "Unknown Title" }
}

/**
 * Parent folder name from a full file path, e.g.
 * "/storage/emulated/0/Music/Aurelius/song.mp3" -> "Aurelius".
 * Returns "Unknown folder" when no usable path is available.
 */
fun folderNameFromPath(path: String?): String {
    val p = path?.trim().orEmpty()
    if (p.isEmpty()) return "Unknown folder"
    val parent = p.substringBeforeLast('/', "")
    val name = parent.substringAfterLast('/', "")
    return name.ifEmpty { "Device storage" }
}

/** Folder name from a MediaStore RELATIVE_PATH, e.g. "Music/Aurelius/" -> "Aurelius". */
fun folderNameFromRelativePath(relativePath: String?): String {
    val parts = relativePath?.trim('/')?.split('/')?.filter { it.isNotBlank() }.orEmpty()
    return parts.lastOrNull() ?: "Unknown folder"
}

/** Scans the device's local audio via MediaStore. Runs on a background thread by callers. */
object LibraryScanner {
    fun scan(context: Context): List<Track> {
        val tracks = mutableListOf<Track>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        context.contentResolver.query(
            uri, projection, selection, null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            // DATA is deprecated since API 29 but still returns the real path on
            // API 27/28 devices (this phone) and is the only folder source there.
            // RELATIVE_PATH exists only on API 29+; guard the column index.
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val relCol = if (android.os.Build.VERSION.SDK_INT >= 29) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else -1
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val rawTitle = cursor.getString(titleCol)
                val rawArtist = cursor.getString(artistCol)
                val rawAlbum = cursor.getString(albumCol)
                val albumId = cursor.getLong(albumIdCol)
                val duration = cursor.getLong(durCol)
                val addedAt = cursor.getLong(addedCol)
                val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                val relPath = if (relCol >= 0) cursor.getString(relCol) else null
                val folder = when {
                    !dataPath.isNullOrBlank() -> folderNameFromPath(dataPath)
                    !relPath.isNullOrBlank() -> folderNameFromRelativePath(relPath)
                    else -> "Unknown folder"
                }
                val contentUri = ContentUris.withAppendedId(uri, id)
                val artUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                )
                tracks += Track(
                    id = "local_$id",
                    title = displayTitle(rawTitle, dataPath),
                    artist = displayArtist(rawArtist),
                    album = displayAlbum(rawAlbum),
                    durationMs = duration,
                    uri = contentUri.toString(),
                    artworkUri = artUri.toString(),
                    isLocal = true,
                    addedAtSec = addedAt,
                    folderName = folder
                )
            }
        }
        return tracks
    }
}
